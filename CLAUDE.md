# IjiriServer

Spring Boot 3.5 / Java 21 / Gradle / PostgreSQL / Spring Data JPA / Spring Security (stateless JWT).
Members sign up with email + password (SMTP email verification) or sign in with Kakao/Google
SDK tokens, which the server verifies before issuing its own JWTs.

## Working rules

- Make the smallest change that fully addresses the request. Follow existing code when it is sound;
  do not refactor unrelated code.
- Before changing code, inspect the relevant implementation, tests, configuration, and this document.
  For cross-cutting changes, inspect all affected areas.
- After changing code, re-read every changed file at least once and check: correctness, SOLID,
  the conventions below, line length, leftovers (unused imports, dead code, stale TODOs). Fix what you find.
- Run the narrowest relevant checks first. Run `./gradlew test` for behavior changes or anything that may
  affect application startup; do not report completion until it passes. Documentation-only changes need
  no tests. If a check cannot run or fails for an environmental reason, report the exact error output;
  never claim it passed.
- Do not narrate routine steps. Give concise progress updates only when work is substantial.
- Act, don't ask, on rule-conforming fixes: when something clearly violates this document or is an
  obvious follow-up of the task, fix it and state it. Ask only when a decision affects product behavior
  or requirements are genuinely ambiguous.
- Final report in Korean, concise, no emoji: what changed, why (the intent), and which checks ran.
- Do not commit unless explicitly asked. For multi-file or multi-concern changes, propose a commit plan
  after the report (see *Commit convention*).
- Use available tools and skills (docs lookup, code review, security review, etc.) when they materially help.
- When you change behavior described in this document (especially *Security notes*), update this document
  in the same change.

## Project structure

Keep this layout. New features follow the same shape. Check the source tree before relying on specific
classes or endpoints; do not create empty packages.

```
ijiri.ijiriserver
├── IjiriServerApplication        (@EnableJpaAuditing, @EnableScheduling live here)
├── domain
│   ├── auth
│   │   ├── common                shared by all providers
│   │   │   ├── client            SocialTokenVerifier, SocialUnlinkClient (strategy interfaces)
│   │   │   ├── dto/response      AuthResponse
│   │   │   ├── exception         AuthStatusCode
│   │   │   └── service           SocialUnlinkService (+ impl)
│   │   ├── email                 POST /auth/signup, /auth/signin, /auth/signout, /auth/password/reset,
│   │   │   │                     /auth/email/verification-code, /auth/email/verification-code/verify
│   │   │   │                     (AuthController, EmailVerificationController)
│   │   │   ├── client  controller  dto/request  entity  repository
│   │   │   ├── scheduler  service  service/impl
│   │   ├── oauth                 POST /auth/signin/oauth (provider + token), OAuthController
│   │   │   ├── controller  dto/request  service  service/impl
│   │   ├── kakao                 client only: KakaoOAuthClient (verify), KakaoUnlinkClient
│   │   ├── google                client only: GoogleOAuthClient (verify)
│   │   └── token                 POST /token/refresh (TokenController)
│   │       ├── controller  dto/request  entity
│   │       ├── repository  scheduler  service  service/impl
│   ├── member                    GET/PATCH/DELETE /members/me
│   │   ├── controller  dto  dto/request  dto/response  entity  event  exception
│   │   ├── repository  scheduler  service  service/impl
│   ├── interestcar               GET/PUT /members/me/interest-cars
│   │   ├── controller  dto/request  dto/response  entity  exception
│   │   ├── repository  service  service/impl
│   ├── carmodel                  GET /car-models, /car-models/{id}; model / generation / trim master
│   │                             imported from resources/car-master/car_master.csv at startup
│   │                             (insert-only); BuildDirection enum
│   ├── ownedcar                  GET/POST /members/me/cars, PATCH/DELETE /members/me/cars/{id}
│   ├── part                      GET /parts/suggest, /parts, /brands (Part, Brand, PartCategory)
│   ├── upload                    POST /uploads/images; ImageStorageClient (LocalImageStorageClient),
│   │                             UploadedImageCleanupScheduler
│   ├── post                      POST /posts, GET/PATCH/DELETE /posts/{id}, GET /feed,
│   │                             GET /members/{id}/posts (PostController, FeedController)
│   ├── wishlist                  POST /wishlist, DELETE /wishlist/{id}, GET /members/me/wishlist
│   ├── block                     POST/DELETE /members/{id}/block, GET /members/me/blocks
│   └── report                    POST /reports; GET /admin/reports, PATCH /admin/reports/{id}
│                                 (AdminReportController)
└── global
    ├── config                    SecurityConfig, SwaggerConfig, ClockConfig (Asia/Seoul Clock + auditing),
    │                             HttpClientConfig (timeouts for external APIs),
    │                             WebConfig (serves local uploads at /images/**)
    ├── entity                    BaseTimeEntity
    ├── exception                 StatusCode, CommonStatusCode, CustomException, GlobalExceptionHandler,
    │                             CustomErrorController (/error -> BaseResponse)
    ├── jwt                       JwtProvider, JwtAuthenticationFilter, JwtCookieManager, SessionValidator
    ├── ratelimit                 RateLimiter (in-memory fixed window)
    ├── response                  BaseResponse
    ├── security                  401/403 handlers returning BaseResponse
    └── validation                @MaxUtf8Bytes
```

Resources: `db/migration` holds the Flyway migrations (`V{n}__description.sql`), `car-master/car_master.csv`
the car master data.

Domain dependencies (no cycles): `post` -> `ownedcar`, `carmodel`, `part`, `upload`, `block`, `wishlist`,
`member`; `report` -> `post`, `member`; `ownedcar`/`interestcar` -> `carmodel`, `member`; `wishlist` -> `part`.
`upload`, `part`, `carmodel` depend on no other domain service. Shared value types used by several domains
(e.g. `BuildDirection`) live in the lowest domain (`carmodel`). Within the `post` aggregate, `post_image` and
`post_part_tag` use JPA relations and FKs; across domains there are only id columns.

Tests: `src/test/resources/application-test.yaml` runs PostgreSQL 17 via Testcontainers (Docker required)
with the same Flyway migrations, plus dummy secrets, so `./gradlew test` needs no local database or `.env`.
`@SpringBootTest` classes use `@ActiveProfiles("test")`. Auth/security changes are covered by MockMvc
integration tests through the real filter chain (`AuthFlowIntegrationTest`; community features in
`CommunityFlowIntegrationTest`); extend them when changing
`SecurityConfig` paths or the auth flow. Rate limits are in memory and shared across tests in one context,
so integration tests give each request its own remote IP.

Package rules:
- A domain package owns its `controller`, `service`, `service/impl`, `repository`, `entity`,
  `dto/request`, `dto/response`, `exception`, and optionally `client`, `scheduler`, `event`.
- Sub-features of a domain get their own sub-package (e.g. `auth/kakao`, `auth/token`).
- DTOs are split into `dto/request` and `dto/response`. Internal transfer objects that are neither go in `dto`.
- Service interfaces live in `service`; implementations live in `service/impl`.
- `global` contains only cross-cutting infrastructure, never business logic.

## Design principles

- **SRP**: external API calls in `client`, orchestration in `service`, HTTP mapping in `controller`.
- **OCP**: add a social provider (e.g. Apple) with a new sub-package containing a `SocialTokenVerifier`
  (returns `MemberRegisterCommand`; plus `SocialUnlinkClient` if needed) and a `Provider` enum value;
  `OAuthService` picks it up automatically. Do not modify existing providers.
- **Cross-domain cleanup** goes through member events; `member` never calls other domains' services
  (no package cycles). `MemberSuspendedEvent` revokes sessions; `MemberProfileImageChangedEvent` lets `upload`
  verify the new profile image (exception rolls the change back) and delete the previous file.
  - `MemberWithdrawnEvent` (soft delete): immediate actions (revoke tokens, social unlink, hide posts).
    Bulk updates in these listeners must not clear the persistence context (`clearAutomatically`), or the
    member change in the same transaction is lost. External API calls listen with
    `@TransactionalEventListener(phase = AFTER_COMMIT)` so they never hold a DB connection; they catch and log failures, and anything that must eventually succeed is retried
    from a `MemberPurgedEvent` listener (e.g. Kakao unlink).
  - `MemberPurgedEvent` (hard delete after 30 days): delete the member's data, including stored files.
    Listeners must be idempotent (a failure rolls back and retries).
- **LSP**: implementations honor the interface contract (same exceptions, no surprises).
- **DIP**: controllers and other services depend on interfaces, never on `*Impl`.

### Service layer

- Default shape: one `XxxService` interface + one `XxxServiceImpl` per domain / feature area.
- Do not split simple CRUD into per-method use-case interfaces (`CreatePostUseCase`, ...) or split
  interfaces just to "apply" ISP. This is a layered architecture; a controller depends on a single service.
- Introduce an abstraction only when multiple implementations exist or a client genuinely needs an
  isolated dependency (e.g. `SocialTokenVerifier`).

## Coding conventions

### Naming
- Service: `XxxService` / `XxxServiceImpl`. Controller: `XxxController` named after the feature.
  Providers have no controllers; they only plug a `client` into `OAuthService`.
- Client: `XxxOAuthClient` or `XxxClient` in `client`.
- Domain event: past-tense `XxxEvent` record in `event`.
- DTO: Java `record`. Requests are `XxxRequest` per endpoint. Responses are **one per domain**
  (`AuthResponse`, `MemberResponse`, `InterestCarResponse`) with static factories per use
  (`AuthResponse.tokens(...)`, `AuthResponse.message(...)`) and `@JsonInclude(NON_NULL)` when fields are
  optional. Do not create a response class per API.
- Status code enum per domain: `XxxStatusCode implements StatusCode`.
- Auth naming: `signin` / `signout` / `signup`, never `login` / `logout`.

### Java style
- Max line length 120. 4-space indentation, no tabs, no wildcard imports, no unused imports.
- Break long lines: method chains one call per line, long parameter lists one per line, 8-space
  continuation indent. When an argument list or annotation is broken, the closing `)` goes on its own line,
  aligned with the opening line. Anything that fits on one line stays on one line.
  ```java
  return new AuthResponse(
          tokens.accessToken(),
          tokens.refreshToken()
  );
  ```
- Constructor injection with `private final` fields via `@RequiredArgsConstructor`, unless the
  constructor needs `@Value` parameters or builds a field from its arguments; then write it by hand.
- Constants: `private static final` UPPER_SNAKE_CASE, only when they add meaning.
- Current time: always `LocalDateTime.now(clock)` with the injected `Clock`, never `now()`.
- External HTTP clients use the `externalApiRequestFactory` bean, never `RestClient.create()`.
- Services never take `HttpServletRequest/Response`; cookies, IPs and headers are handled in controllers.
- Transactions: class-level `@Transactional(readOnly = true)` on query-heavy impls, method-level
  `@Transactional` on writes.
- Comments only where intent is not obvious (why, not what). Korean is allowed.

### Entities and schema
- Every persisted field except `@Id` has `@Column` with `name` set, plus only non-default constraints
  (`nullable = false`, `length` when not 255, `updatable = false`). Never write JPA defaults.
  ```java
  @Column(name = "email", nullable = false)
  @Column(name = "profile_image_url", length = 500)
  ```
- Lombok set, exactly: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`,
  `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, class-level `@Builder`. When creation needs logic
  (e.g. hashing), add a static factory using the builder (`RefreshToken.of(...)`).
- Extend `BaseTimeEntity` when timestamps are needed; enums use `@Enumerated(EnumType.STRING)`.
- Tables and columns: singular snake_case (`member`, `refresh_token`). Constraints: `uk_<table>_<columns>`,
  `idx_<table>_<columns>`, declared in `@Table(uniqueConstraints/indexes)` and in the migration with the
  same name. Never use `@Column(unique = true)`.
- Schema changes: add Flyway migration `V{n+1}__description.sql` in the same change as the entity.
  Never edit an applied migration. `ddl-auto` is `validate` in every profile. No CHECK constraints on
  enum columns.

### API
- Every response, success and error, goes through `BaseResponse<T>`.
  - Success: `BaseResponse.ok(data)` or `BaseResponse.of(XxxStatusCode.SOME_SUCCESS, data)`;
    `data` is always a response DTO, never `null`.
  - Error: `BaseResponse.onFailure(statusCode[, detail])`, produced only by `GlobalExceptionHandler` and
    the handlers in `global/security`. Only these (and `CustomErrorController`) return
    `ResponseEntity<BaseResponse<?>>`.
  - Never return raw DTOs, entities, `ResponseEntity<Dto>`, `void`, or Spring's default error body.
- Controller-facing service methods return the domain's response DTO (use `XxxResponse.message(...)`
  when there is nothing else). Internal service-to-service methods may return primitives or `void`.
- Status codes: `<DOMAIN><HTTP status>[<seq>]`, e.g. `AUTH200`, `AUTH4011`, `MEMBER404`.
- Business errors: `throw new CustomException(XxxStatusCode.SOME_ERROR)`.
- Validate request bodies with `@Valid` + Bean Validation on the request record.
- Authenticated member id: `@Parameter(hidden = true) @AuthenticationPrincipal String memberId`.
- Document with `@Tag` and `@Operation` (multi-line, one attribute per line). Public endpoints use empty
  `@SecurityRequirements` and must be permitted in `SecurityConfig`. Apply security documentation at the
  narrowest accurate scope when endpoints in one controller differ.

## Security notes

These are invariants. Do not change them without being asked, and keep this section in sync with the code.

- JWT subject = member id. Access token 1h, refresh token 28d (`application.yaml`).
- Tokens are returned in the body and as `accessToken` / `refreshToken` cookies (HttpOnly, Secure,
  SameSite=None) via `JwtCookieManager`. Access token: `Authorization` header first, then cookie.
  Refresh: body first, then cookie.
- CSRF: cookie tokens are ignored when the request's `Origin` is not in `cors.allowed-origins`.
- One session per account: every issue deletes the member's existing refresh tokens before saving the new one,
  and `refresh_token.member_id` is unique, so concurrent sign-ins leave one session (the loser gets 409).
- Session model: `refresh_token.session_id` = refresh `jti` = access `sid` claim. `JwtAuthenticationFilter`
  accepts an access token only while its session row exists (`SessionValidator`), so deleting refresh tokens
  (sign-out, login elsewhere, withdrawal) invalidates paired access tokens immediately.
- Refresh tokens are stored as SHA-256 hashes only and rotated on reissue (atomic delete). A token no longer
  stored is simply rejected (no revoke-all). Expired rows are purged daily by `RefreshTokenCleanupScheduler`.
- Sign-out (`/auth/signout`, permitted in `SecurityConfig`): with a valid access token, delete all the
  member's refresh tokens; otherwise delete the session of the refresh token from the body or cookie
  (so sign-out works after the access token expires; apps send it in the body, web sends the HttpOnly
  cookie). Expire both cookies and answer 200 (`AUTH2002`) whenever a token was sent, even if its session
  is already gone. With no token at all the caller is not signed in: 401 `AUTH4012` (same as refresh). Anonymous authentication
  is disabled, so `@AuthenticationPrincipal` is `null` when unauthenticated.
  Withdrawal also expires the cookies.
- Withdrawal is a soft delete (`member.deleted_at`) that also clears `member.email` and turns the member's
  posts `AUTHOR_WITHDRAWN` (hidden). A withdrawn member cannot sign in or re-register with
  the same account for 30 days (`MEMBER403`); `getById` excludes withdrawn members (refresh is `AUTH4012`,
  not 404). `MemberPurgeScheduler` hard-deletes after 30 days, one transaction per member,
  including posts, uploaded image files, owned/interest cars, wishlist, blocks and reports. Kakao unlink runs
  after the withdrawal commit and is retried before the purge if it fails.
- Member identity = `provider + provider_member_id` (unique). Email members use `provider = EMAIL`,
  `provider_member_id = email`; passwords are BCrypt hashes.
- Email sign-up: send code -> verify code -> sign up. 6-digit code bound to the email, valid 5 min,
  5 attempts, 60 s resend cooldown (resend resets verification; the row is locked against concurrent resends).
  Verification gives 30 min to sign up; sign-up atomically consumes the verified row, creates the member and
  signs in (`isNewMember = true`). For an already registered email the send API answers identically and only
  a notice mail is sent (no account enumeration).
- Email sign-in errors are always `AUTH4013`. 5 failures per email (registered or not) within 15 min lock that
  email until the window ends (`AUTH4293`); success or password reset clears the count.
- Rate limits (`RateLimiter`): sign-in 30 / 15 min per IP; verification-code 10 / h per IP; image upload
  100 / h per member. Unknown-email sign-in still runs a BCrypt compare.
- Password reset: send code with `purpose = RESET_PASSWORD` -> verify -> `POST /auth/password/reset`.
  Verification rows are unique per (email, purpose). For an email that is not an active email member the send
  API answers identically and sends nothing. Reset consumes the verification, changes the password and deletes
  all the member's sessions in one transaction.
- Suspension: `PATCH /admin/reports/{id}` with `SUSPEND_MEMBER` sets `member.suspended_until` and deletes the
  member's sessions; `TokenService.issue` rejects suspended members (`MEMBER4031`), covering every sign-in and
  refresh.
- Admin: `/admin/**` requires role `ADMIN` (from the access token's `role` claim; a role change applies after
  the next sign-in). Admins are assigned directly in the database.
- Public GET endpoints: `/car-models/**`, `/feed`, `/posts/{id}`, `/members/{id}/posts`, `/images/**`.
  When authenticated they also exclude posts of members in a block relation (either direction).
- Uploads: max 10 MB, format detected from file signature (JPEG, PNG, WebP, HEIC), random UUID file names.
  Images not attached to a post or profile within 24 h are deleted with their files. EXIF GPS is stripped by
  the app before upload.
- Sign-up fields: email, password, nickname 2-12 chars. Required consents are client-gated only.
- Passwords: 8-20 chars, at least one letter and one digit, at most 72 UTF-8 bytes (`@MaxUtf8Bytes(72)`).
- Social emails are stored only when the provider marks them verified; `member.email` is nullable.
  Social nicknames: longer than 12 are cut to 12; missing or shorter than 2 get `이지리오너` + 4 digits.
- Kakao API: only 400/401 mean an invalid provider token (`AUTH4011`); other errors are `AUTH502`.
- Secrets come from `.env` (never commit it). Never log passwords, tokens, or secrets.
- Profiles: `local` (default; show-sql, dev JWT secret fallback), `prod` (`SPRING_PROFILES_ACTIVE=prod`;
  every secret required, no fallbacks, forwarded headers for client IP), `test` (Testcontainers PostgreSQL, tests only).
  All profiles run Flyway and `ddl-auto: validate`.

## Operational notes

Not problems today; revisit when the deployment changes.

- `RateLimiter` and the registered-email notice cooldown are in memory: with several instances each counts
  separately. Move them to Redis when scaling out.
- Every authenticated request looks up `session_id` once; add a cache if traffic grows.
- `forward-headers-strategy: native` trusts `X-Forwarded-For` only from Tomcat's internal proxy ranges. If the
  load balancer IP is outside them, every request looks like the LB IP and per-IP limits hit all users.
  Check against the actual deployment.
- Uploaded images are stored on local disk (`storage.local.base-dir`) and served by the app. With several
  instances or ephemeral disks, add an S3-compatible `ImageStorageClient` and drop `WebConfig`. Post images
  store absolute URLs, so changing `storage.public-base-url` needs a data migration.
- Part suggest uses `pg_trgm` similarity (keyword + typo tolerance). Vector (embedding) search is not
  implemented yet; it needs an embedding provider and `pgvector`.
- If Kakao unlink fails after withdrawal, the link stays visible in the user's Kakao account until the purge
  retry (up to 30 days).

## Commit convention

- Commit messages always in English.
- Format: `<type>: <summary>`, imperative mood, no scope, no trailing period, max 72 chars.
- Types: `feat`, `fix`, `refactor`, `chore` (build/config), `docs`, `test`, `style`.
- One commit = one logical change. Group files by change, not by file type.
- Commit plan format: ready-to-paste commands, one `git add -A <paths>` + `git commit` pair per commit,
  ordered so every commit compiles. Each command on a single line (no `\` continuations).
  ```bash
  git add -A src/main/java/ijiri/ijiriserver/domain/auth/token
  git commit -m "refactor: move RefreshTokenServiceImpl to service/impl"
  ```

## Commands

- Build & test: `./gradlew test` (Docker must be running; no local DB or `.env` needed).
  CI must use a runner with Docker (e.g. GitHub Actions `ubuntu-latest`).
- Run: `./gradlew bootRun` (requires `.env` with DB settings)