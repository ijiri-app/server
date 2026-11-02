#!/usr/bin/env bash
# 무중단(blue/green) 배포. 서비스 4개마다 쉬고 있는 색에 새 이미지를 띄우고, 모두 응답하면 nginx 를 그쪽으로 돌린 뒤 이전 색을 내린다.
# 하나라도 뜨지 못하면 nginx 는 그대로 두고 새 컨테이너만 내린다 (서비스 중인 버전은 영향 없음).
# 사용: ./deploy.sh <이미지 태그>   예) ./deploy.sh sha-<커밋>   되돌리기도 이전 커밋의 SHA 태그로 똑같이 실행한다
set -euo pipefail
cd "$(dirname "$0")"

TAG=${1:?usage: ./deploy.sh <image tag>}
IMAGE_REPO=ghcr.io/ijiri-app/server
UPSTREAM=nginx/upstream.conf
KEEP_IMAGES=3
SERVICES=(user community car media)
declare -A PORT=(
    [user-blue]=8081 [user-green]=8091
    [community-blue]=8082 [community-green]=8092
    [car-blue]=8083 [car-green]=8093
    [media-blue]=8084 [media-green]=8094
)
declare -A CURRENT NEXT

current_color() {
    grep -oE "server $1-(blue|green)" "$UPSTREAM" 2>/dev/null | head -n 1 | sed "s/^server $1-//" || true
}

# 포트에 HTTP 응답이 오면(상태 코드는 상관없음) 기동이 끝난 것이다. Flyway 마이그레이션은 그 전에 끝난다
wait_started() {
    local container=$1
    for _ in $(seq 1 90); do
        if [ -z "$(docker compose ps -q --status running "$container")" ]; then
            return 1
        fi
        local code
        code=$(curl -s -o /dev/null -m 2 -w '%{http_code}' "http://127.0.0.1:${PORT[$container]}/" || true)
        if [ "$code" != 000 ]; then
            return 0
        fi
        sleep 2
    done
    return 1
}

docker compose up -d --wait db-user db-community db-car db-media kafka

for s in "${SERVICES[@]}"; do
    CURRENT[$s]=$(current_color "$s")
    if [ "${CURRENT[$s]}" = blue ]; then NEXT[$s]=green; else NEXT[$s]=blue; fi
    echo "$s: current ${CURRENT[$s]:-none}, deploying $TAG to ${NEXT[$s]}"
    IMAGE_TAG=$TAG docker compose up -d --no-deps --force-recreate --pull missing "$s-${NEXT[$s]}"
done

started=true
for s in "${SERVICES[@]}"; do
    if ! wait_started "$s-${NEXT[$s]}"; then
        echo "$s-${NEXT[$s]} failed to start" >&2
        docker compose logs --tail 100 "$s-${NEXT[$s]}" >&2
        started=false
    fi
done
if [ "$started" != true ]; then
    echo "keeping the current colors live" >&2
    for s in "${SERVICES[@]}"; do
        docker compose stop "$s-${NEXT[$s]}"
    done
    exit 1
fi

previous_upstream=$(cat "$UPSTREAM" 2>/dev/null || true)
{
    for s in "${SERVICES[@]}"; do
        printf 'upstream %s {\n    server %s-%s:8080;\n    keepalive 16;\n}\n' "$s" "$s" "${NEXT[$s]}"
    done
} > "$UPSTREAM.tmp"
mv "$UPSTREAM.tmp" "$UPSTREAM"

# 첫 배포라면 여기서 nginx 와 터널이 처음 뜬다 (upstream.conf 가 있어야 nginx 가 뜬다)
docker compose up -d nginx cloudflared
if ! docker compose exec -T nginx nginx -t || ! docker compose exec -T nginx nginx -s reload; then
    echo "nginx reload failed; restoring upstream" >&2
    if [ -n "$previous_upstream" ]; then
        printf '%s\n' "$previous_upstream" > "$UPSTREAM"
        docker compose exec -T nginx nginx -s reload || true
    fi
    for s in "${SERVICES[@]}"; do
        docker compose stop "$s-${NEXT[$s]}"
    done
    exit 1
fi
echo "nginx now routes to: $(for s in "${SERVICES[@]}"; do printf '%s-%s ' "$s" "${NEXT[$s]}"; done)"

# reload 전에 받은 요청은 이전 nginx worker 가 마저 처리한다. 잠깐 기다린 뒤 이전 색을 내린다
sleep 5
for s in "${SERVICES[@]}"; do
    if [ -n "${CURRENT[$s]}" ]; then
        docker compose stop "$s-${CURRENT[$s]}"
    fi
done

# 되돌리기용으로 서비스마다 최근 이미지 몇 개만 남긴다 (사용 중인 이미지는 rmi 가 거절한다)
for s in "${SERVICES[@]}"; do
    docker image ls "$IMAGE_REPO-$s" --format '{{.Repository}}:{{.Tag}}' \
        | grep -v ':<none>$' \
        | tail -n +$((KEEP_IMAGES + 1)) \
        | xargs -r docker rmi >/dev/null 2>&1 || true
done
docker image prune -f >/dev/null

echo "deployed $TAG"
