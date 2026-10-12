# Coding conventions

## Naming
- Service: `XxxService` / `XxxServiceImpl`. Controller: `XxxController` named after the feature.
  Providers have no controllers; they only plug a `client` into `OAuthService`.
- Client: `XxxOAuthClient` or `XxxClient` in `client`.
- Domain event: past-tense `XxxEvent` record in `event`.
- DTO: Java `record`. Requests are `XxxRequest` per endpoint. Responses are **one per domain**
  (`AuthResponse`, `MemberResponse`, `InterestCarResponse`) with static factories per use
  (`AuthResponse.tokens(...)`, `AuthResponse.message(...)`) and `@JsonInclude(NON_NULL)` when fields are
  optional. Do not create a response class per API.
- Status code enum per domain: `XxxStatusCode implements StatusCode`.
- Auth naming in code: `signIn` / `signOut` / `signup`. URL paths follow the feature spec
  (`/auth/login`, `/auth/login/email`, `/auth/logout`, `/auth/refresh`).

## Java style
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
    `remainingAttempts`) go in `result` via `new CustomException(code, Map.of(...))`.
  - `ResponseEntity` only in the error handlers, `CustomErrorController`, `POST /wishlist` (201 vs 200) and
    `ImageFileController` (stands in for the storage: raw file upload/download, no `BaseResponse`).
  - Never return raw DTOs, entities or Spring's default error body.
- Status codes: the spec's code names (`INVALID_CREDENTIALS`, `NOT_FOUND`, `VALIDATION_FAILED`, ...). Generic
  cases reuse the generic name (`NOT_FOUND` for every 404, `FORBIDDEN` for every 403 permission error);
  domain-only cases use an UPPER_SNAKE name (`NICKNAME_ALREADY_EXISTS`). The app picks text by `code`.
- Times are serialized as ISO 8601 with the Korean offset (`2026-09-30T20:00:00+09:00`, `JacksonConfig`).
- Business errors: `throw new CustomException(XxxStatusCode.SOME_ERROR)`.
- Validate request bodies with `@Valid` + Bean Validation on the request record.
- Authenticated member id: `@Parameter(hidden = true) @AuthenticationPrincipal String memberId`.
- Document with `@Tag` and `@Operation` (multi-line, one attribute per line). Public endpoints use empty
  `@SecurityRequirements` and must be permitted in `SecurityConfig`. Apply security documentation at the
  narrowest accurate scope when endpoints in one controller differ.
