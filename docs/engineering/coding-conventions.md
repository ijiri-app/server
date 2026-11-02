# Coding conventions

## Naming
- Use case: one `VerbNounUseCase` per operation, implemented by `XxxService`. Outbound: `XxxPort` / `XxxPersistenceAdapter`, `XxxJpaRepository`.
  Controller: `XxxController` named after the feature.
  Providers have no controllers; they only plug an `adapter/out/external` client into `OAuthService`.
- Client: `XxxOAuthClient` or `XxxClient` in `adapter/out/external`.
- Domain event: past-tense `XxxEvent` data class in `event`.
- DTO: Kotlin `data class`. Requests are `XxxRequest` per endpoint. Responses are **one per domain**
  (`AuthResponse`, `MemberResponse`, `InterestCarResponse`) with `companion object` factories per use
  (`AuthResponse.tokens(...)`, `AuthResponse.message(...)`) and `@JsonInclude(NON_NULL)` when fields are
  optional. Do not create a response class per API.
- Status code enum per domain: `enum class XxxStatusCode(...) : StatusCode`.
- Auth naming in code: `signIn` / `signOut` / `signup`. URL paths follow the feature spec
  (`/auth/login`, `/auth/login/email`, `/auth/logout`, `/auth/refresh`).

## Kotlin style
- Kotlin official code style. Max line length 120. 4-space indentation, no tabs, no wildcard imports,
  no unused imports. No Lombok; sources live in `src/main/kotlin` and `src/test/kotlin`.
- Break long lines: method chains one call per line, long parameter lists one per line. When an argument or
  parameter list is broken, the closing `)` goes on its own line, aligned with the opening line, and the last
  element has a trailing comma. Anything that fits on one line stays on one line.
  ```kotlin
  return AuthResponse(
      accessToken = tokens.accessToken,
      refreshToken = tokens.refreshToken,
  )
  ```
- Constructor injection with `private val` primary constructor parameters. `@Value` parameters escape the
  dollar sign: `@Value("\${jwt.secret}") private val secret: String`.
- `kotlin-spring` opens Spring components and `allOpen` opens JPA entities; never add `open` by hand.
- Nullability is part of the contract: use a nullable type only when null is a real value. Ports, use cases
  and repositories return `T?` instead of `Optional<T>`; use `findByIdOrNull` for Spring Data lookups.
- Constants: `private const val` UPPER_SNAKE_CASE in a `companion object` at the end of the class, only when
  they add meaning. Loggers: `private val log = LoggerFactory.getLogger(Xxx::class.java)` in the same
  `companion object`. Utility code with only static members is an `object`.
- Prefer Kotlin collection functions over Java streams and `listOf`/`mapOf`/`setOf` over `List.of` and friends.
- Current time: always `LocalDateTime.now(clock)` with the injected `Clock`, never `now()`.
- External HTTP clients use the `externalApiRequestFactory` bean, never `RestClient.create()`. The one
  exception is the AWS SDK (`R2ImageStorageClient`), which has its own HTTP client; set its timeouts there.
- Services never take `HttpServletRequest/Response`; cookies, IPs and headers are handled in controllers.
- Transactions: class-level `@Transactional(readOnly = true)` on query-heavy impls, method-level
  `@Transactional` on writes.
- Comments only where intent is not obvious (why, not what). Korean is allowed.

## Entities and schema
- Every persisted field except `@Id` has `@Column` with `name` set, plus only non-default constraints
  (`nullable = false`, `length` when not 255, `updatable = false`). Never write JPA defaults.
  ```java
  @Column(name = "email", nullable = false)
  @Column(name = "profile_image_url", length = 500)
  ```
- Entities are regular classes (no data classes, no `equals`/`hashCode`). The primary constructor takes every
  persisted field as a plain parameter (named arguments replace builders; nullable fields default to `null`,
  `id: Long = 0L` last). Each field is a body property initialized from it with a `protected set`, so state
  changes only through entity methods. Generated ids are non-null `Long` (`0` means not yet saved).
  ```kotlin
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = id
      protected set
  ```
  When creation needs logic (e.g. hashing), add a `companion object` factory (`RefreshToken.of(...)`).
- Extend `BaseTimeEntity` when timestamps are needed; enums use `@Enumerated(EnumType.STRING)`.
- Tables and columns: singular snake_case (`member`, `refresh_token`). Constraints: `uk_<table>_<columns>`,
  `idx_<table>_<columns>`, declared in `@Table(uniqueConstraints/indexes)` and in the migration with the
  same name. Never use `@Column(unique = true)`.
- Schema changes: add Flyway migration `V{n+1}__description.sql` in the same change as the entity.
  Never edit an applied migration. `ddl-auto` is `validate` in every profile. No CHECK constraints on
  enum columns.

## API
- Paths, request/response fields and error code names follow the feature spec (기능 명세); every response
  body still goes through `BaseResponse<T>` (`{code, message, result}`).
  - Success with data: `BaseResponse.ok(data)` or `BaseResponse.of(XxxStatusCode.SOME_SUCCESS, data)`;
    `data` is always a response DTO, never `null`. Where the spec says 204, the controller method is `void`
    with `@ResponseStatus(HttpStatus.NO_CONTENT)` and the service returns `void`.
  - Lists: `{items, nextCursor}`; `nextCursor` is a string (the last id), `null` on the last page; requests
    take `?cursor=&size=` (size max 50).
  - Error: `BaseResponse.onFailure(statusCode[, detail])`, produced only by `GlobalExceptionHandler` and
    the handlers in `global/security`. Extra error fields (`fields`, `retryAfterSeconds`,
    `remainingAttempts`) go in `result` via `CustomException(code, mapOf(...))`.
  - `ResponseEntity` only in the error handlers, `CustomErrorController`, `POST /wishlist` (201 vs 200) and
    `ImageFileController` (stands in for the storage: raw file upload/download, no `BaseResponse`).
  - Never return raw DTOs, entities or Spring's default error body.
- Status codes: the spec's code names (`INVALID_CREDENTIALS`, `NOT_FOUND`, `VALIDATION_FAILED`, ...). Generic
  cases reuse the generic name (`NOT_FOUND` for every 404, `FORBIDDEN` for every 403 permission error);
  domain-only cases use an UPPER_SNAKE name (`NICKNAME_ALREADY_EXISTS`). The app picks text by `code`.
- Times are serialized as ISO 8601 with the Korean offset (`2026-09-30T20:00:00+09:00`, `JacksonConfig`).
- Business errors: `throw CustomException(XxxStatusCode.SOME_ERROR)`.
- Validate request bodies with `@Valid` + Bean Validation on the request data class. Request properties are
  nullable so a missing field fails validation (`VALIDATION_FAILED`) instead of JSON parsing; constraints use
  the field target (`@field:NotBlank`), container elements keep type annotations (`List<@Valid X>?`).
- Authenticated member id: `@Parameter(hidden = true) @AuthenticationPrincipal memberId: String`.
- Document with `@Tag` and `@Operation` (multi-line, one attribute per line). Public endpoints use empty
  `@SecurityRequirements` and must be permitted in `SecurityConfig`. Apply security documentation at the
  narrowest accurate scope when endpoints in one controller differ.
