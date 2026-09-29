# IjiriServer

Spring Boot 3.5 / Java 21 / Gradle / PostgreSQL / Spring Data JPA / Spring Security (stateless JWT).
Mobile apps log in with Kakao/Google SDK tokens; the server verifies them and issues its own JWTs.

## Workflow (mandatory)

1. **Output while working = code only.** During work, produce only code creation/modification.
   Do not narrate the process step by step.
2. **Review at least once.** After finishing any logic, re-read every changed file at least once
   and check: correctness, DDD boundaries, SOLID, conventions below, line length, leftovers
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
│   │   ├── common                shared login logic
│   │   │   ├── dto               internal DTOs (e.g. SocialUserInfo)
│   │   │   ├── dto/response
│   │   │   ├── exception         AuthStatusCode
│   │   │   └── service           SocialLoginService (+ impl)
│   │   ├── kakao                 POST /auth/kakao
│   │   │   ├── client  controller  dto/request  service  service/impl
│   │   ├── google                POST /auth/google
│   │   │   ├── client  controller  dto/request  service  service/impl
│   │   └── token                 POST /auth/reissue, /auth/logout
│   │       ├── controller  dto/request  dto/response  entity
│   │       ├── repository  scheduler  service  service/impl
│   └── member                    /members/me
│       ├── controller  dto/response  entity  exception
│       ├── repository  service  service/impl
└── global
    ├── config                    SecurityConfig, SwaggerConfig
    ├── entity                    BaseTimeEntity
    ├── exception                 StatusCode, CommonStatusCode, CustomException, GlobalExceptionHandler
    ├── jwt                       JwtProvider, JwtAuthenticationFilter
    ├── response                  BaseResponse
    └── security                  401/403 handlers returning BaseResponse
```

Package rules:
- A domain package owns its `controller`, `service`, `service/impl`, `repository`, `entity`,
  `dto/request`, `dto/response`, `exception`, and optionally `client`, `scheduler`.
- Sub-features of a domain get their own sub-package (e.g. `auth/kakao`, `auth/token`).
- DTOs are always split into `dto/request` and `dto/response`.
  Internal transfer objects that are neither go directly in `dto`.
- Service interfaces live in `service`; implementations live in `service/impl`.
- `global` contains only cross-cutting infrastructure, never business logic.

## Design principles

### DDD
- Each domain is a bounded context. Other domains are accessed through their **service
  interfaces**, not their repositories (e.g. `member` revokes tokens via `TokenRevokeService`,
  never `RefreshTokenRepository`).
- Put behavior that belongs to an entity on the entity (e.g. `RefreshToken.isExpired()`).
  Services orchestrate; entities hold their own invariants.
- Entities: no public setters. Change state through intention-revealing methods.
- Never return entities from controllers; map to a response DTO (`XxxResponse.from(entity)`).

### SOLID
- **SRP**: one reason to change per class. External API calls go in `client`,
  orchestration in `service`, HTTP mapping in `controller`.
- **OCP**: add a new social provider by adding a new sub-package (`client` + service)
  that feeds `SocialLoginService`; do not modify existing providers.
- **LSP**: implementations must honor the interface contract (same exceptions, no surprises).
- **ISP**: split service interfaces by client need (e.g. `TokenIssueService`,
  `TokenReissueService`, `LogoutService`, `TokenRevokeService`). One impl may implement
  several interfaces; callers depend only on the interface they use.
- **DIP**: controllers and other services depend on interfaces, never on `*Impl`.

## Coding conventions

### Naming
- Service interface: `XxxService`, describing one capability (`MemberQueryService`,
  `MemberWithdrawService`). Implementation: `XxxServiceImpl` in `service/impl`.
- Controller: `XxxController`; for auth providers `XxxAuthController`.
- External API client: `XxxOAuthClient` / `XxxClient` in `client`.
- DTO: `XxxRequest` / `XxxResponse`, implemented as Java `record`.
- Status code enum per domain: `XxxStatusCode implements StatusCode`.
- Tables: snake_case singular (`member`, `refresh_token`). Unique/index names:
  `uk_<table>_<columns>`, `idx_<table>_<columns>`.

### Java style
- Max line length **120**. Break long lines: method chains one call per line,
  long parameter lists one per line, aligned with 8-space continuation indent.
- 4-space indentation, no tabs. No wildcard imports. Remove unused imports.
- Constructor injection only via Lombok `@RequiredArgsConstructor` with `private final` fields.
- Entities: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`,
  `@Builder` on a private constructor, extend `BaseTimeEntity` when timestamps are needed,
  enums stored with `@Enumerated(EnumType.STRING)`.
- Transactions: class-level `@Transactional(readOnly = true)` on query-heavy impls,
  method-level `@Transactional` on writes.
- Constants: `private static final` UPPER_SNAKE_CASE.
- Comments: only where intent is not obvious from code (why, not what). Korean is allowed.

### API
- **Every API response — success AND error — MUST be returned through `BaseResponse<T>`.**
  - Success: `BaseResponse.ok(data)` or `BaseResponse.of(XxxStatusCode.SOME_SUCCESS, data)`
    (no data: `BaseResponse.of(XxxStatusCode.SOME_SUCCESS, null)`).
  - Error: `BaseResponse.onFailure(statusCode[, detail])`, produced only by
    `GlobalExceptionHandler` and the Spring Security handlers in `global/security`.
  - Never return raw DTOs, entities, `ResponseEntity<Dto>`, `void`, or Spring's default
    error body from any endpoint.
- Status code format: `<DOMAIN><HTTP status>[<seq>]`, e.g. `AUTH200`, `AUTH4011`, `MEMBER404`.
- Validate request bodies with `@Valid` + Bean Validation on the request record.
- Business errors: `throw new CustomException(XxxStatusCode.SOME_ERROR)`.
  Never return error responses manually from controllers; `GlobalExceptionHandler` handles them.
- Authenticated member id: `@Parameter(hidden = true) @AuthenticationPrincipal String memberId`.
- Document endpoints with `@Tag` and `@Operation`. Public endpoints use `@SecurityRequirements`
  (empty) and must be permitted in `SecurityConfig`.

## Security notes
- JWT subject = member id. Access token 1h, refresh token 28d (`application.yaml`).
- Refresh tokens are stored as SHA-256 hashes only, rotated on reissue,
  and expired rows are purged daily by `RefreshTokenCleanupScheduler`.
- Member identity = `provider + provider_user_id` (unique constraint).
- Secrets come from `.env` (never commit it). Never log tokens or secrets.

## Commit convention

- **Commit messages MUST always be written in English** (even though reports are in Korean).
- Format: `<type>(<scope>): <summary>` — Conventional Commits, imperative mood,
  no trailing period, max 72 chars.
- Types: `feat`, `fix`, `refactor`, `chore` (build/config), `docs`, `test`, `style`.
- Scope: domain or area — `auth`, `kakao`, `google`, `token`, `member`, `global`, `config`, `build`.
- One commit = one logical change. Group files by that change, not by file type.
- Commit plan format in the report:
  ```
  1. refactor(token): move RefreshTokenServiceImpl to service/impl
     - src/main/java/.../auth/token/service/impl/RefreshTokenServiceImpl.java
  2. docs: add commit convention to CLAUDE.md
     - CLAUDE.md
  ```

## Commands
- Build & test: `./gradlew test`
- Run: `./gradlew bootRun` (requires `.env` with DB settings)
