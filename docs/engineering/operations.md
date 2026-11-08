# Operations and verification

## Profiles and tests

- Profiles (each service has its own `application*.yaml`): `local` (default; show-sql, dev secrets, localhost
  defaults for its DB, Kafka and the other services), `prod` (`SPRING_PROFILES_ACTIVE=prod`; every secret
  required, no fallbacks, forwarded headers for client IP, springdoc disabled), `test` (embedded PostgreSQL,
  Kafka listeners not started, tests only). All profiles run Flyway and `ddl-auto: validate`.
- Every service reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `CORS_ALLOWED_ORIGINS`
  and the `<NAME>_SERVICE_URL` of the services it calls; `service-user` also reads the JWT, mail and OAuth
  variables, `service-media` the storage variables.

Tests: each service's `src/test/resources/application-test.yaml` runs PostgreSQL 17 as an embedded process
(Zonky, no Docker) with that service's Flyway migrations, plus dummy secrets, so `./gradlew test` needs no local
database, Kafka or `.env`. An in-memory database such as H2 cannot replace it: `service-car` uses `pg_trgm`
(`word_similarity`, GIN trigram indexes). `@SpringBootTest` classes use `@ActiveProfiles("test")` and
`@AutoConfigureEmbeddedDatabase`; the user service's tests also `@Import(SynchronousTaskExecutorConfig::class)`,
which runs the verification mail executor inline so a test can verify the mail mock right after the request.
Other services and Kafka are replaced by `@MockitoBean` on the client classes (`UserServiceClient`,
`CarServiceClient`, `MediaServiceClient`, the user service's readers) and `MessagePublisher`; Kafka consumers are
tested by calling the listener method with a JSON payload.
Auth/security changes are covered by MockMvc integration tests through the real filter chain
(`AuthFlowIntegrationTest` in service-user, which simulates nginx by calling `/internal/auth/verify` and copying
the result headers; `CommunityFlowIntegrationTest`, `CarFlowIntegrationTest`, `MediaFlowIntegrationTest` set
`X-Member-Id` / `X-Member-Role` directly); extend them when changing a `SecurityConfig`, the gateway contract or
the auth flow. Rate limits are in memory and shared across tests in one context, so integration tests give each
request its own remote IP or member id.

## Commands

- Build & test: `./gradlew test` (all modules), `./gradlew :service-car:test` (one module). Nothing else needed.
- Run everything locally: `cp .env.example .env`, fill it, then `docker compose --profile blue up -d --build`
  and write `nginx/upstream.conf` pointing every upstream at the blue slot (or run `./deploy.sh` with a tag after
  `docker compose build`). The gateway listens on `http://localhost:8080`, the internal listener on
  `http://localhost:8090`, Kafka on `localhost:19092`, the databases on `localhost:5432..5435`.
- Run one service from Gradle against compose: start the databases, Kafka and nginx with compose, then
  `./gradlew :service-user:bootRun` (the `local` profile points at `localhost:5432`, `localhost:19092` and
  `http://localhost:8090/<service>`). Requests to a service started this way must carry `X-Member-Id`
  (and `X-Member-Role`) themselves; Swagger UI (`/swagger-ui.html` on the service port) has a header scheme for it.
- Deploy: `./deploy.sh <image tag>` (e.g. `sha-<commit>`) in the deploy directory. Compose runs four PostgreSQL 17
  containers with pgvector (`db-user`, `db-community`, `db-car`, `db-media`), Kafka (KRaft, single node), two slots
  per service (`user-blue` / `user-green`, `community-*`, `car-*`, `media-*`; compose profiles, so a plain
  `docker compose up -d` starts none of them), nginx and Cloudflare Tunnel. The tunnel's public hostname points to
  `http://nginx:80`; nginx routes by path to the slot named in `nginx/upstream.conf`, which `deploy.sh` writes for
  all four services at once, and serves the internal listener on port 81 for calls between services.
  `deploy.sh` first removes containers no longer in the compose file (`--remove-orphans`, e.g. the monolith's
  `app-*` and `db`; volumes stay), starts the new image of every service on its idle slot, waits until
  `/actuator/health/readiness` (readiness state and DB) answers 200 on its localhost port (blue 8081-8084,
  green 8091-8094), switches nginx with one reload and smoke-tests `GET /car-models` through the public
  listener (gateway auth plus a service). If the smoke test fails, nginx goes back to the previous slots, which
  are still running. Otherwise it removes the old slots after a short drain (Spring graceful shutdown,
  `stop_grace_period: 40s`) and deletes every local image except the deployed tag (including the monolith's
  `ghcr.io/ijiri-app/server`); a rollback pulls its tag from GHCR again. If any slot is not ready, nginx is left
  untouched and the new slots are stopped. Only `health` is exposed under `/actuator`, and nginx answers 404 for
  `/actuator/` on the public listener.
  nginx listens on 127.0.0.1:8080 (public) and 127.0.0.1:8090 (internal), the slots on their localhost ports,
  Kafka on 127.0.0.1:19092 and the databases on 5432-5435, all on the host's localhost only; only the tunnel
  exposes the API. Remove everything with `docker compose --profile '*' down`.
- Blue/green runs old and new versions of every service side by side for a moment, both colors consume the same
  Kafka group and both apply to the same databases: Flyway migrations must stay compatible with the previous
  version (add first, drop or rename in a later release), `/internal` contracts and Kafka messages must stay
  readable by the previous version, and `@Scheduled` jobs must stay idempotent.
- CI/CD (`.github/workflows/ci-cd.yml`): PRs to `develop`/`main` and pushes run `./gradlew test` (a new push to
  a PR cancels its previous run; branch pushes are never cancelled). Pushes skip docs-only changes and, in
  parallel with the tests, build every service jar with the Gradle cache, then build the four images at once
  (`--target ci --build-arg MODULE=<module>`) and push `ghcr.io/ijiri-app/server-<name>:sha-<commit>` for
  `user`, `community`, `car` and `media`. Only when both pass does `promote` copy the manifests to the branch tag
  (`:<branch>`), so an image whose tests failed is never tagged or deployed; pushes to `main` then deploy (only when the repository
  variable `DEPLOY_ENABLED` is `true`) through the self-hosted runner labeled `ijiri-server`, which copies
  `docker-compose.yml`, `deploy.sh`, `nginx/default.conf` and `nginx/proxy.inc` into the repository variable
  `DEPLOY_DIR` (where `.env` and `nginx/upstream.conf` live) and runs `./deploy.sh sha-<commit>`. Deploys run
  one at a time (`deploy-production` concurrency group, never cancelled). Roll back or redeploy with
  Actions > CI/CD > Run workflow on `main` with `image_tag` = `sha-<older commit>` (or `./deploy.sh` in
  `DEPLOY_DIR`); all four services move together. Dependabot opens weekly PRs to `develop` for Actions, Gradle
  and the Docker base images.

## Operational notes

Not problems today; revisit when the deployment changes.

- Memory on the home server: four JVMs (512 MB limit each, eight during a deploy), four PostgreSQL containers and
  Kafka (768 MB limit, 256 MB heap). Expect about 3 GB at rest and 5 GB during a deploy.
- `RateLimiter` and the sign-in lock are in memory per service instance; both colors of a service count
  separately during a deploy. Move them to Redis when scaling out.
- Every request through the gateway costs `service-user` two queries (session row, member role) before the
  target service runs; add a cache if traffic grows.
- Internal calls go through the nginx internal listener, so a service is unreachable while nginx reloads or
  before the first deploy wrote `nginx/upstream.conf`. Callers answer `SERVICE_UNAVAILABLE` (502) meanwhile.
- Kafka delivers at least once. A listener that still fails after about a minute of backoff moves the message to
  `<topic>.<consumer group>.DLT` (e.g. `member.purged.service-media.DLT`). A message left there means a purged
  member's data or files remain, so check the DLT topics after incidents
  (`kafka-console-consumer.sh --topic <dlt> --from-beginning` in the kafka container) and replay them to the
  original topic once the cause is fixed. Unsent messages wait in `outbox_message` (service-user,
  service-community) while the broker is down; a growing table means the relay is failing.
- `forward-headers-strategy: native` trusts `X-Forwarded-For` only from Tomcat's internal proxy ranges. nginx and
  cloudflared are on the compose network, so the client IP arrives; check again if the proxies move.
- Images: prod uses Cloudflare R2 (`storage.type = r2`, `R2_*` env) behind a custom domain
  (`STORAGE_PUBLIC_BASE_URL`, e.g. `https://img.ijiri.net`; `r2.dev` is not for production). The bucket needs
  a lifecycle rule deleting `tmp/` objects after 1 day, and CORS allowing PUT from the app if web uploads are
  added. Post images and profile images store absolute URLs, so changing the base URL needs a data migration.
- DB backups: daily `pg_dump` of all four databases to the second home server over an encrypted VPN
  (e.g. Tailscale), kept at most 30 days so withdrawn members' data does not outlive the 30-day promise in
  backups; restore-test once. Kafka data needs no backup (messages are consumed within seconds).
- Verification mails go out on the user service's `applicationTaskExecutor` (Spring Boot default: 8 threads,
  unbounded queue). Shutdown waits up to 10 s for sends in flight (`spring.task.execution.shutdown`), within
  the compose `stop_grace_period` of 40 s.
- Part suggest uses keyword/alias match plus `pg_trgm` similarity. Vector (embedding) search from the spec
  is not implemented yet; it needs a multilingual embedding provider and `pgvector` in `db-car`.
- If Kakao unlink fails after withdrawal, the link stays visible in the user's Kakao account until the purge
  retry (up to 30 days).
