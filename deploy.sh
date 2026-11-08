#!/usr/bin/env bash
# 무중단(blue/green) 배포. 서비스 4개마다 쉬고 있는 색에 새 이미지를 띄우고, 모두 응답하면 nginx 를 그쪽으로 돌린 뒤 이전 색을 내린다.
# 하나라도 준비되지 못하면 nginx 는 그대로 두고 새 컨테이너만 내린다 (서비스 중인 버전은 영향 없음).
# nginx 를 돌린 뒤 게이트웨이를 거친 요청(smoke test)이 실패하면 이전 색으로 되돌린다.
# 사용: ./deploy.sh <이미지 태그>   예) ./deploy.sh sha-<커밋>   되돌리기도 이전 커밋의 SHA 태그로 똑같이 실행한다
set -euo pipefail
cd "$(dirname "$0")"

TAG=${1:?usage: ./deploy.sh <image tag>}
IMAGE_REPO=ghcr.io/ijiri-app/server
UPSTREAM=nginx/upstream.conf
SERVICES=(user community car media)
# macOS 기본 bash 3.2 는 연관 배열(declare -A)이 없어 SERVICES 와 같은 순서의 일반 배열을 쓴다
CURRENT=()
NEXT=()

port_of() {
    case "$1" in
        user-blue) echo 8081 ;; user-green) echo 8091 ;;
        community-blue) echo 8082 ;; community-green) echo 8092 ;;
        car-blue) echo 8083 ;; car-green) echo 8093 ;;
        media-blue) echo 8084 ;; media-green) echo 8094 ;;
    esac
}

current_color() {
    grep -oE "server $1-(blue|green)" "$UPSTREAM" 2>/dev/null | head -n 1 | sed "s/^server $1-//" || true
}

# readiness 가 200(UP)이면 요청을 받을 준비가 된 것이다 (Flyway 마이그레이션, DB 연결 포함)
wait_ready() {
    local container=$1
    for _ in $(seq 1 90); do
        if [ -z "$(docker compose ps -q --status running "$container")" ]; then
            return 1
        fi
        if curl -fs -o /dev/null -m 2 "http://127.0.0.1:$(port_of "$container")/actuator/health/readiness"; then
            return 0
        fi
        sleep 2
    done
    return 1
}

# 공개 리스너로 user(게이트웨이 인증)와 car 를 함께 거치는 공개 API. 200 이 아니면 라우팅이 깨진 것이다
smoke_test() {
    for _ in $(seq 1 5); do
        if curl -fs -o /dev/null -m 5 "http://127.0.0.1:8080/car-models"; then
            return 0
        fi
        sleep 2
    done
    return 1
}

stop_next() {
    for i in "${!SERVICES[@]}"; do
        docker compose stop "${SERVICES[$i]}-${NEXT[$i]}"
    done
}

# --remove-orphans: compose 파일에서 빠진 컨테이너(모놀리식 app-blue/green, 단일 db)를 지운다. 볼륨은 남는다
docker compose up -d --wait --remove-orphans db-user db-community db-car db-media kafka

for i in "${!SERVICES[@]}"; do
    s=${SERVICES[$i]}
    CURRENT[$i]=$(current_color "$s")
    if [ "${CURRENT[$i]}" = blue ]; then NEXT[$i]=green; else NEXT[$i]=blue; fi
    echo "$s: current ${CURRENT[$i]:-none}, deploying $TAG to ${NEXT[$i]}"
    IMAGE_TAG=$TAG docker compose up -d --no-deps --force-recreate --pull missing "$s-${NEXT[$i]}"
done

started=true
for i in "${!SERVICES[@]}"; do
    s=${SERVICES[$i]}
    if ! wait_ready "$s-${NEXT[$i]}"; then
        echo "$s-${NEXT[$i]} is not ready" >&2
        docker compose logs --tail 100 "$s-${NEXT[$i]}" >&2
        started=false
    fi
done
if [ "$started" != true ]; then
    echo "keeping the current colors live" >&2
    stop_next
    exit 1
fi

previous_upstream=$(cat "$UPSTREAM" 2>/dev/null || true)
{
    for i in "${!SERVICES[@]}"; do
        s=${SERVICES[$i]}
        printf 'upstream %s {\n    server %s-%s:8080;\n    keepalive 16;\n}\n' "$s" "$s" "${NEXT[$i]}"
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
    stop_next
    exit 1
fi
echo "nginx now routes to: $(for i in "${!SERVICES[@]}"; do printf '%s-%s ' "${SERVICES[$i]}" "${NEXT[$i]}"; done)"

# 이전 색은 아직 떠 있으므로, 게이트웨이를 거친 요청이 실패하면 nginx 를 이전 색으로 되돌린다
if ! smoke_test; then
    echo "smoke test failed" >&2
    if [ -n "$previous_upstream" ]; then
        echo "restoring the previous colors" >&2
        printf '%s\n' "$previous_upstream" > "$UPSTREAM"
        docker compose exec -T nginx nginx -s reload || true
        stop_next
    fi
    exit 1
fi

# reload 전에 받은 요청은 이전 nginx worker 가 마저 처리한다. 잠깐 기다린 뒤 이전 색을 내리고 컨테이너를 지운다
# (멈춘 컨테이너가 남아 있으면 그 이미지를 지울 수 없다)
sleep 5
for i in "${!SERVICES[@]}"; do
    s=${SERVICES[$i]}
    if [ -n "${CURRENT[$i]}" ]; then
        docker compose rm -sf "$s-${CURRENT[$i]}"
    fi
done

# 지금 띄운 태그 말고는 이미지를 남기지 않는다. 되돌릴 때는 ./deploy.sh <이전 SHA 태그> 가 GHCR 에서 다시 받는다.
# 모놀리식 시절 이미지(ghcr.io/ijiri-app/server)도 함께 지운다
for repo in "$IMAGE_REPO" "${SERVICES[@]/#/$IMAGE_REPO-}"; do
    docker image ls "$repo" --format '{{.Repository}}:{{.Tag}}' \
        | grep -v -e ':<none>$' -e ":$TAG\$" \
        | xargs -r docker rmi >/dev/null 2>&1 || true
done
docker image prune -f >/dev/null

echo "deployed $TAG"
