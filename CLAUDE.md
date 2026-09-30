# IjiriServer

Spring Boot 3.5 / Java 21 / Gradle / PostgreSQL / Spring Data JPA / Spring Security (stateless JWT).
Members sign up with email + password (SMTP email verification) or sign in with Kakao/Google
SDK tokens, which the server verifies before issuing its own JWTs.

## Workflow (mandatory)

1. **Output while working = code only.** During work, produce only code creation/modification.
   Do not narrate the process step by step.
2. **Review at least once.** After finishing any logic, re-read every changed file at least once
   and check: correctness, SOLID, conventions below, line length, leftovers
   (unused imports, dead code, stale TODOs). Fix what the review finds.
3. **Verify with `./gradlew test`.** Every change must pass both compilation and runtime
   (context load + tests). Do not report completion until it passes. If it cannot pass
   (e.g. DB not reachable), say so explicitly with the error output.
4. **Final report in Korean.** Only after all code is done, explain concisely in Korean
   what changed and *why* (the development intent). No emoji.
5. **Commit plan after the report.** After the Korean report, propose commits split by
   logical unit, listing the files in each commit and its message (see *Commit convention*).
   Only propose; do not run `git commit` unless explicitly asked.
6. **Act, don't ask, on rule-conforming fixes.** When something clearly violates this document
   (wrong package, naming, convention) or is an obvious follow-up of the task, fix it directly
   and state it ("~하겠습니다") instead of asking "~할까요?". Ask only for genuine product
   or design decisions that this document does not settle.
7. **Use tools/skills.** Whenever an available tool or skill fits the task
   (docs lookup, code review, simplify, security review, etc.), use it.

## Project structure

Keep this layout. New features follow the same shape.

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
│   │   ├── email                 POST /auth/signup, /auth/signin, /auth/signout,
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
│   ├── member                    GET/DELETE /members/me
│   │   ├── controller  dto  dto/response  entity  event  exception
│   │   ├── repository  scheduler  service  service/impl
│   └── interestcar               PUT /members/me/interest-cars
│       ├── controller  dto/request  dto/response  entity  exception
│       ├── repository  service  service/impl
└── global
    ├── config                    SecurityConfig, SwaggerConfig, ClockConfig (Asia/Seoul Clock + auditing),
    │                             HttpClientConfig (timeouts for external APIs)
    ├── entity                    BaseTimeEntity
    ├── exception                 StatusCode, CommonStatusCode, CustomException, GlobalExceptionHandler,
    │                             CustomErrorController (/error -> BaseResponse)
    ├── jwt                       JwtProvider, JwtAuthenticationFilter, JwtCookieManager,
    │                             SessionValidator
    ├── ratelimit                 RateLimiter (in-memory fixed window)
    ├── response                  BaseResponse
    ├── security                  401/403 handlers returning BaseResponse
    └── validation                @MaxUtf8Bytes
```

Resources: `db/migration` holds the Flyway migrations (`V{n}__description.sql`).
Tests: `src/test/resources/application-test.yaml` (H2 in PostgreSQL mode + dummy secrets), so
`./gradlew test` needs neither a database nor `.env`. `@SpringBootTest` classes use
`@ActiveProfiles("test")`.

Package rules:
- A domain package owns its `controller`, `service`, `service/impl`, `repository`, `entity`,
  `dto/request`, `dto/response`, `exception`, and optionally `client`, `scheduler`, `event`.
- Sub-features of a domain get their own sub-package (e.g. `auth/kakao`, `auth/token`).
- DTOs are always split into `dto/request` and `dto/response`.
  Internal transfer objects that are neither go directly in `dto`.
- Service interfaces live in `service`; implementations live in `service/impl`.
- `global` contains only cross-cutting infrastructure, never business logic.

## Design principles

### SOLID
- **SRP**: one reason to change per class. External API calls go in `client`,
  orchestration in `service`, HTTP mapping in `controller`.
- **OCP**: add a new social provider (e.g. Apple) by adding a sub-package with a
  `SocialTokenVerifier` (returns `MemberRegisterCommand`; and `SocialUnlinkClient` if needed)
  implementation plus a `Provider` enum value; `OAuthService` picks it up automatically.
  Do not modify existing providers.
- **Cross-domain cleanup** goes through member events; `member` never calls other domains'
  services (no package cycles).
  - `MemberWithdrawnEvent` (soft delete moment): things that must happen immediately
    (revoke tokens, social unlink, hide posts). External API calls listen with
    `@TransactionalEventListener(phase = AFTER_COMMIT)` so they never hold a DB connection;
    they catch and log failures, and anything that must eventually succeed is retried from a
    `MemberPurgedEvent` listener (e.g. Kakao unlink).
  - `MemberPurgedEvent` (row hard-deleted after 30 days): delete the member's data, including
    uploaded files in storage. Listeners must be idempotent (a failure rolls back and retries).
- **LSP**: implementations must honor the interface contract (same exceptions, no surprises).
- **ISP**: applied pragmatically, not mechanically. See *Service layer* — do not split a
  service into per-method interfaces just to satisfy ISP.
- **DIP**: controllers and other services depend on interfaces, never on `*Impl`.

## Service layer

- Default shape: one `XxxService` interface + one `XxxServiceImpl` per domain / feature area
  (e.g. `MemberService`, `TokenService`, `InterestCarService`, `OAuthService`).
- Do not split simple CRUD into per-method use-case interfaces (`CreatePostUseCase`,
  `UpdatePostUseCase`, ...). Create such interfaces only when there is an explicit
  architectural need.
- Do not split interfaces just to "apply" SOLID/ISP. This is a layered (3-tier) architecture:
  a controller depends on a single service, so use-case splitting adds files without giving
  hexagonal-style boundaries.
- Introduce an abstraction only when multiple implementations actually exist or a client
  genuinely needs an isolated dependency (e.g. `SocialTokenVerifier` with Kakao/Google
  implementations).

## Coding conventions

### Naming
- Service interface: `XxxService`, one per domain / feature area (`MemberService`).
  Implementation: `XxxServiceImpl` in `service/impl`.
- Domain event: past-tense `XxxEvent` record in `event` (`MemberWithdrawnEvent`).
- Controller: `XxxController` named after the feature (`AuthController`, `OAuthController`,
  `EmailVerificationController`, `TokenController`). Providers have no controllers; they only
  plug a `client` into `OAuthService`.
- External API / infrastructure client: `XxxOAuthClient` or `XxxClient` in `client`
  (`KakaoOAuthClient`, `KakaoUnlinkClient`, `VerificationMailClient`).
- DTO: Java `record`. Requests are `XxxRequest` per endpoint. Responses are **one per domain**
  (`AuthResponse`, `MemberResponse`, `InterestCarResponse`) with static factories per use
  (`AuthResponse.tokens(...)`, `AuthResponse.message(...)`) and `@JsonInclude(NON_NULL)` when
  fields are optional. Do not create a new response class per API.
- Status code enum per domain: `XxxStatusCode implements StatusCode`.
- Tables: snake_case singular (`member`, `refresh_token`). Unique/index names:
  `uk_<table>_<columns>`, `idx_<table>_<columns>`, declared in `@Table(uniqueConstraints/indexes)`
  and in the migration with the same name. Never use `@Column(unique = true)` (auto-generated name).

### Java style
- Max line length **120**. Break long lines: method chains one call per line,
  long parameter lists one per line, aligned with 8-space continuation indent.
- When an argument list or annotation is broken across lines, the closing `)` goes on its
  own line, aligned with the line that opened it. Calls/annotations that fit on one line
  stay on one line.
  ```java
  return new AuthResponse(
          tokens.accessToken(),
          tokens.refreshToken()
  );

  @Operation(
          summary = "...",
          description = "..."
  )
  ```
- 4-space indentation, no tabs. No wildcard imports. Remove unused imports.
- Constructor injection only, with `private final` fields. Use Lombok `@RequiredArgsConstructor`
  unless the constructor needs `@Value` parameters or builds a field from its arguments
  (e.g. `Map<Provider, SocialTokenVerifier>`, a configured `RestClient`); then write it by hand.
- Entities: **every persisted field except the primary key (`@Id`) MUST have `@Column`** with
  `name` always set, plus only the constraints that differ from JPA defaults
  (`nullable = false`, `length` when not 255, `updatable = false`).
  Never write JPA default values: `nullable = true`, `length = 255`, `unique = false`,
  `updatable = true`, `insertable = true`.
  ```java
  @Column(name = "email", nullable = false)
  @Column(name = "profile_image_url", length = 500)
  ```
- Entities use exactly this Lombok set: `@Getter`,
  `@NoArgsConstructor(access = AccessLevel.PROTECTED)`,
  `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, class-level `@Builder`.
  When creation needs logic (e.g. hashing), add a static factory that uses the builder
  (`RefreshToken.of(...)`). Extend `BaseTimeEntity` when timestamps are needed; store enums
  with `@Enumerated(EnumType.STRING)`.
- Schema changes: add a new Flyway migration `V{n+1}__description.sql` in the same change as the
  entity change. Never edit a migration that has been applied. `ddl-auto` is `validate` in every
  profile. Do not add CHECK constraints for enum columns (adding an enum value would need a migration).
- Transactions: class-level `@Transactional(readOnly = true)` on query-heavy impls,
  method-level `@Transactional` on writes.
- Constants: `private static final` UPPER_SNAKE_CASE.
- Current time: always `LocalDateTime.now(clock)` with the injected `Clock`, never `now()`.
- External HTTP clients use the `externalApiRequestFactory` bean (timeouts), never `RestClient.create()`.
- Services never take `HttpServletRequest/Response`; cookies, IPs and headers are handled in controllers.
- Comments: only where intent is not obvious from code (why, not what). Korean is allowed.

### API
- **Every API response — success AND error — MUST be returned through `BaseResponse<T>`.**
  - Success: `BaseResponse.ok(data)` or `BaseResponse.of(XxxStatusCode.SOME_SUCCESS, data)`
    — `data` is always a response DTO, never `null`.
  - Error: `BaseResponse.onFailure(statusCode[, detail])`, produced only by
    `GlobalExceptionHandler` and the Spring Security handlers in `global/security`.
  - Never return raw DTOs, entities, `ResponseEntity<Dto>`, `void`, or Spring's default
    error body from any endpoint. Only the error handlers (`GlobalExceptionHandler`,
    `CustomErrorController`) return `ResponseEntity<BaseResponse<?>>` to set the HTTP status.
- Every service method called by a controller returns its domain's response DTO, never `void`
  or a primitive/wrapper. When there is nothing else to return, return a message
  (e.g. `AuthResponse.message(...)`). Never return entities from controllers. Internal service-to-service
  methods (e.g. `existsEmailMember`) may return primitives or `void`.
- Status code format: `<DOMAIN><HTTP status>[<seq>]`, e.g. `AUTH200`, `AUTH4011`, `MEMBER404`.
- Validate request bodies with `@Valid` + Bean Validation on the request record.
- Business errors: `throw new CustomException(XxxStatusCode.SOME_ERROR)`.
  Never return error responses manually from controllers; `GlobalExceptionHandler` handles them.
- Authenticated member id: `@Parameter(hidden = true) @AuthenticationPrincipal String memberId`.
- Document endpoints with `@Tag` and `@Operation`. `@Operation` is always written multi-line,
  one attribute (`summary`, `description`) per line, closing `)` on its own line.
- Public endpoints use `@SecurityRequirements` (empty) and must be permitted in `SecurityConfig`.

## Security notes
- JWT subject = member id. Access token 1h, refresh token 28d (`application.yaml`).
- Tokens are returned in the body AND as `accessToken` / `refreshToken` cookies
  (HttpOnly, Secure, SameSite=None) via `JwtCookieManager`. Access token is read from the
  `Authorization` header first, then the cookie. Refresh reads the body, then the cookie.
- CSRF: cookie tokens are ignored when the request has an `Origin` header that is not in
  `cors.allowed-origins`.
- Sign-out is an authenticated endpoint (`@AuthenticationPrincipal`): expire both cookies and
  delete all of the member's refresh tokens. Withdrawal also expires the cookies.
- One session per account (no concurrent login): every issue deletes the member's existing
  refresh tokens before saving the new one.
- Each login is a session (`refresh_token.session_id` = refresh `jti` = access `sid` claim).
  `JwtAuthenticationFilter` accepts an access token only while its session row exists
  (`SessionValidator`, one indexed lookup per request), so deleting refresh tokens (sign-out,
  login on another device, withdrawal) also invalidates the paired access tokens immediately.
  Refresh tokens are stored as SHA-256 hashes only,
  rotated on reissue (atomic delete); a token that is no longer stored is just rejected (no
  revoke-all, which would sign out the device that just logged in). Expired rows are purged
  daily by `RefreshTokenCleanupScheduler`.
- Withdrawal is a soft delete (`member.deleted_at`). A withdrawn member cannot sign in or
  re-register with the same account for 30 days (`MEMBER403`); `getById` excludes withdrawn
  members (refresh of a withdrawn member's token is `AUTH4012`, not 404). `MemberPurgeScheduler`
  hard-deletes them after 30 days, one transaction per member. Kakao unlink runs after the
  withdrawal commit; if it fails it is retried before the purge (unlinking twice is harmless).
- Member identity = `provider + provider_member_id` (unique constraint). Email members use
  `provider = EMAIL`, `provider_member_id = email`; passwords are BCrypt hashes.
- Email sign-up is 3 steps: send code -> verify code -> sign up. Code: 6 digits bound to that
  email, valid 5 min, 5 attempts, 60 s resend cooldown (resend resets verification;
  the row is locked so concurrent resends cannot send two codes).
  Verifying marks the row verified and gives 30 min to sign up; sign-up atomically consumes
  the verified row (one verification = one sign-up), creates the member and signs in
  (tokens in body + cookies, `isNewMember = true`). For an already registered email the
  send API answers identically and only a notice mail is sent (no account enumeration).
- Rate limits (`RateLimiter`): sign-in 10 / 15 min per email and 30 / 15 min per IP,
  verification-code 10 / h per IP. Unknown-email sign-in still runs a BCrypt compare.
- Sign-up fields: email, password, nickname 2-12 chars. Required consents are gated by the
  client only (not sent or stored).
- Passwords: 8-20 chars, at least one letter and one digit,
  and at most 72 UTF-8 bytes (`@MaxUtf8Bytes(72)`, BCrypt limit).
- Social emails are stored only when the provider marks them verified; `member.email` is nullable.
  Social nicknames follow the same 2-12 chars rule: longer ones are cut to 12, missing or shorter
  ones get a default (`이지리오너` + 4 digits).
- Kakao API: only 400/401 mean an invalid provider token (`AUTH4011`); other errors (e.g. 429)
  are `SOCIAL_SERVER_ERROR` (`AUTH502`).
- Use `signin` / `signout` / `signup` naming for auth, never `login` / `logout`.
- Secrets come from `.env` (never commit it). Never log tokens or secrets.
- Profiles: `local` (default; show-sql, dev JWT secret fallback), `prod`
  (set `SPRING_PROFILES_ACTIVE=prod`; every secret required, forwarded headers for client IP)
  and `test` (H2, used by tests only). All profiles run Flyway and `ddl-auto: validate`.

## Commit convention

- **Commit messages MUST always be written in English** (even though reports are in Korean).
- Format: `<type>: <summary>` — imperative mood, no scope (never `type(scope):`),
  no trailing period, max 72 chars.
- Types: `feat`, `fix`, `refactor`, `chore` (build/config), `docs`, `test`, `style`.
- One commit = one logical change. Group files by that change, not by file type.
- Commit plan format in the report: ready-to-paste git commands, one `git add` + `git commit`
  pair per commit, in an order where every commit compiles. Use `git add -A <paths>` so
  deletions and renames are included. Each command on a single line (never `\` line
  continuations; pasted trailing spaces break them), one code block per command.
  ```bash
  git add -A src/main/java/ijiri/ijiriserver/domain/auth/token
  git commit -m "refactor: move RefreshTokenServiceImpl to service/impl"

  git add CLAUDE.md
  git commit -m "docs: add commit convention to CLAUDE.md"
  ```

## Commands
- Build & test: `./gradlew test`
- Run: `./gradlew bootRun` (requires `.env` with DB settings)
