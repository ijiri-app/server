# Operations and verification

## Profiles and tests

- Profiles: `local` (default; show-sql, dev JWT secret fallback), `prod` (`SPRING_PROFILES_ACTIVE=prod`;
  every secret required, no fallbacks, forwarded headers for client IP), `test` (Testcontainers
  PostgreSQL, tests only).
  All profiles run Flyway and `ddl-auto: validate`.

Tests: `src/test/resources/application-test.yaml` runs PostgreSQL 17 via Testcontainers (Docker required)
with the same Flyway migrations, plus dummy secrets, so `./gradlew test` needs no local database or `.env`.
`@SpringBootTest` classes use `@ActiveProfiles("test")`. Auth/security changes are covered by MockMvc
integration tests through the real filter chain (`AuthFlowIntegrationTest`; community features in
`CommunityFlowIntegrationTest`); extend them when changing
`SecurityConfig` paths or the auth flow. Rate limits are in memory and shared across tests in one context,
so integration tests give each request its own remote IP.

## Commands

- Build & test: `./gradlew test` (Docker must be running; no local DB or `.env` needed).
  CI must use a runner with Docker (e.g. GitHub Actions `ubuntu-latest`).
- Run: `./gradlew bootRun` (requires `.env` with DB settings)
- Deploy: `cp .env.example .env`, fill it, then `docker compose up -d` (app image from GHCR + PostgreSQL 17
  with pgvector + Cloudflare Tunnel; the image runs the `prod` profile; add `--build` to build locally). Only
  the tunnel exposes the API; app and DB listen on 127.0.0.1 of the host.
- CI/CD (`.github/workflows/ci-cd.yml`): pushes and PRs to `develop`/`main` run `./gradlew test`; pushes then
  build and push `ghcr.io/ijiri-app/server:<branch>` and `:sha-<commit>`; pushes to `main` deploy through the
  self-hosted runner labeled `ijiri-server` on the server, which copies `docker-compose.yml` into the repository
  variable `DEPLOY_DIR` (where `.env` lives), pulls the image and restarts. Roll back by running
  `IMAGE_TAG=sha-<commit> docker compose up -d` in `DEPLOY_DIR`.

## Operational notes

Not problems today; revisit when the deployment changes.

- `RateLimiter`, the sign-in lock and the silent reset-code cooldown are in memory: with several instances each counts
  separately. Move them to Redis when scaling out.
- Every authenticated request looks up `session_id` once; add a cache if traffic grows.
- `forward-headers-strategy: native` trusts `X-Forwarded-For` only from Tomcat's internal proxy ranges. If the
  load balancer IP is outside them, every request looks like the LB IP and per-IP limits hit all users.
  Check against the actual deployment.
- Images: prod uses Cloudflare R2 (`storage.type = r2`, `R2_*` env) behind a custom domain
  (`STORAGE_PUBLIC_BASE_URL`, e.g. `https://img.ijiri.net`; `r2.dev` is not for production). The bucket needs
  a lifecycle rule deleting `tmp/` objects after 1 day, and CORS allowing PUT from the app if web uploads are
  added. Post images store absolute URLs, so changing the base URL needs a data migration.
- DB backups: daily `pg_dump` to the second home server over an encrypted VPN (e.g. Tailscale), kept at most
  30 days so withdrawn members' data does not outlive the 30-day promise in backups; restore-test once.
- Part suggest uses keyword/alias match plus `pg_trgm` similarity. Vector (embedding) search from the spec
  is not implemented yet; it needs a multilingual embedding provider and `pgvector`.
- If Kakao unlink fails after withdrawal, the link stays visible in the user's Kakao account until the purge
  retry (up to 30 days).
