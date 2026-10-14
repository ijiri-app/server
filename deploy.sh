#!/usr/bin/env bash
# 무중단(blue/green) 배포. 쉬고 있는 색에 새 이미지를 띄우고, 응답하면 nginx 를 그쪽으로 돌린 뒤 이전 색을 내린다.
# 새 버전이 뜨지 못하면 nginx 는 그대로 두고 새 컨테이너만 내린다 (서비스 중인 버전은 영향 없음).
# 사용: ./deploy.sh <이미지 태그>   예) ./deploy.sh sha-<커밋>   되돌리기도 이전 커밋의 SHA 태그로 똑같이 실행한다
set -euo pipefail
cd "$(dirname "$0")"

TAG=${1:?usage: ./deploy.sh <image tag>}
IMAGE_REPO=ghcr.io/ijiri-app/server
UPSTREAM=nginx/upstream.conf
KEEP_IMAGES=3
declare -A PORT=([blue]=8081 [green]=8082)

current=$(grep -oE 'app-(blue|green)' "$UPSTREAM" 2>/dev/null | head -n 1 | sed 's/^app-//' || true)
if [ "$current" = blue ]; then next=green; else next=blue; fi
echo "current: ${current:-none}, deploying $TAG to $next"

docker compose up -d --wait db
IMAGE_TAG=$TAG docker compose up -d --no-deps --force-recreate --pull missing "app-$next"

# 포트에 HTTP 응답이 오면(상태 코드는 상관없음) 기동이 끝난 것이다. Flyway 마이그레이션은 그 전에 끝난다
started=false
for _ in $(seq 1 90); do
    if [ -z "$(docker compose ps -q --status running "app-$next")" ]; then
        break
    fi
    code=$(curl -s -o /dev/null -m 2 -w '%{http_code}' "http://127.0.0.1:${PORT[$next]}/" || true)
    if [ "$code" != 000 ]; then
        started=true
        break
    fi
    sleep 2
done

if [ "$started" != true ]; then
    echo "app-$next failed to start; keeping ${current:-nothing} live" >&2
    docker compose logs --tail 100 "app-$next" >&2
    docker compose stop "app-$next"
    exit 1
fi

previous_upstream=$(cat "$UPSTREAM" 2>/dev/null || true)
cat > "$UPSTREAM.tmp" <<EOF
upstream app {
    server app-$next:8080;
    keepalive 16;
}
EOF
mv "$UPSTREAM.tmp" "$UPSTREAM"

# 첫 배포라면 여기서 nginx 와 터널이 처음 뜬다 (upstream.conf 가 있어야 nginx 가 뜬다)
docker compose up -d nginx cloudflared
if ! docker compose exec -T nginx nginx -t || ! docker compose exec -T nginx nginx -s reload; then
    echo "nginx reload failed; restoring upstream" >&2
    if [ -n "$previous_upstream" ]; then
        printf '%s\n' "$previous_upstream" > "$UPSTREAM"
        docker compose exec -T nginx nginx -s reload || true
    fi
    docker compose stop "app-$next"
    exit 1
fi
echo "nginx now routes to app-$next"

# reload 전에 받은 요청은 이전 nginx worker 가 마저 처리한다. 잠깐 기다린 뒤 이전 색을 내린다
if [ -n "$current" ]; then
    sleep 5
    docker compose stop "app-$current"
fi

# 되돌리기용으로 최근 이미지 몇 개만 남긴다 (사용 중인 이미지는 rmi 가 거절한다)
docker image ls "$IMAGE_REPO" --format '{{.Repository}}:{{.Tag}}' \
    | grep -v ':<none>$' \
    | tail -n +$((KEEP_IMAGES + 1)) \
    | xargs -r docker rmi >/dev/null 2>&1 || true
docker image prune -f >/dev/null

echo "deployed $TAG on app-$next"
