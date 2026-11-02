# Architecture

## Project structure

Keep this layout. New features follow the same shape. Check the source tree before relying on specific
classes or endpoints; do not create empty packages.

```
ijiri.ijiriserver
├── IjiriServerApplication        (@EnableJpaAuditing, @EnableScheduling live here)
├── domain
│   ├── auth
│   │   ├── common                shared by all providers: SocialTokenVerifier, SocialUnlinkClient
│   │   │                         (outbound ports), kakao and google under adapter/out/external,
│   │   │                         AuthResponse, AuthStatusCode, SocialUnlinkService
│   │   ├── email                 POST /auth/signup, /auth/login/email, /auth/logout,
│   │   │   │                     /auth/password/reset/code, /auth/password/reset,
│   │   │   │                     /auth/email/send-code, /auth/email/verify-code
│   │   │   │                     (AuthController, EmailVerificationController)
│   │   ├── oauth                 POST /auth/login (provider + token), OAuthController
│   │   └── token                 POST /auth/refresh (TokenController)
│   ├── member                    GET/PATCH/DELETE /members/me, GET /members/{id}
│   ├── interestcar               GET/PUT /members/me/interest-cars
│   ├── carmodel                  GET /car-models, /car-models/{id}; model / generation / trim master
│   │                             imported from resources/car-master/car_master.csv at startup
│   │                             (insert-only); BuildStyle enum
│   ├── ownedcar                  GET/POST /members/me/cars, PATCH/DELETE /members/me/cars/{id}
│   ├── part                      GET /parts/suggest, /parts, /brands (Part, Brand, PartCategory)
│   ├── upload                    POST /uploads/images (presigned upload URLs); ImageStorageClient
│   │                             (R2ImageStorageClient for prod; DbImageStorageClient for local/test
│   │                             with ImageFileController: PUT /uploads/files/**, GET /images/**),
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
    │                             JacksonConfig (LocalDateTime as ISO 8601 with +09:00)
    ├── entity                    BaseTimeEntity
    ├── data                      BaseResponse
    ├── exception                 exception/CustomException, status_code/StatusCode, CommonStatusCode
    ├── handler                   GlobalExceptionHandler, CustomErrorController (/error -> BaseResponse),
    │                             401/403 handlers, BaseResponseWriter
    ├── jwt                       JwtProvider, JwtAuthenticationFilter, JwtCookieManager, SessionValidator
    ├── ratelimit                 RateLimiter (in-memory fixed window)
    ├── security                  AdminVerifier
    ├── storage                   ImageUrlResolver (image key <-> public URL)
    └── validation                @MaxUtf8Bytes
```

Resources: `db/migration` holds the Flyway migrations (`V{n}__description.sql`), `car-master/car_master.csv`
the car master data.

Domain dependencies (no cycles): `post` -> `ownedcar`, `carmodel`, `interestcar`, `part`, `upload`, `block`,
`member`; `wishlist` -> `post`, `member`; `report` -> `post`, `member`; `ownedcar`/`interestcar` -> `carmodel`,
`member`. `upload`, `part`, `carmodel` depend on no other domain service. When a lower domain needs data from a
higher one, it declares a small reader interface that the higher domain implements with a persistence
adapter (`MemberPostCounter`, `MemberWishCounter`, `OwnedCarPostCounter`, `PostWishReader`, implemented by
`PostCountProvider` and `WishCountProvider`); higher domains notify lower ones with events
(`PostDeletedEvent`, `PostPartsRemovedEvent`). Shared value types used by several domains
(e.g. `BuildStyle`) live in the lowest domain (`carmodel`). Within the `post` aggregate, `post_image` and
`post_part_tag` use JPA relations and FKs; across domains there are only id columns.

## Package ownership

The server is a single application with a hexagonal structure inside each feature under `domain`:

```
domain/<feature>
├── adapter
│   ├── in/web/controller      HTTP
│   ├── in/scheduler           scheduled jobs
│   ├── out/persistence        XxxJpaRepository, XxxPersistenceAdapter, providers for other domains' ports
│   └── out/external           external API, mail and storage clients
├── application
│   ├── data                   commands and results; data/request, data/response
│   ├── port/in                XxxUseCase
│   ├── port/out               XxxPort, client and reader interfaces
│   └── service                XxxService (implements XxxUseCase)
├── entity                     JPA entities, enums and value types
├── event
└── exception
```

- Dependencies point inward: `application` never imports `adapter`; `entity`, `event` and `exception` import
  neither `application` nor `adapter`. Controllers and other domains depend on `port/in` use cases.
- Services use outbound ports, never `XxxJpaRepository` or a concrete client. A port declares only the
  methods in use. JPA entities double as the domain model; they are not mapped to separate classes.
- Sub-features of a domain get their own sub-package (e.g. `auth/email`, `auth/token`).
- `global` contains only cross-cutting infrastructure, never business logic.

## Design principles

- **SRP**: external API calls in `adapter/out/external`, orchestration in `application/service`, HTTP mapping in `controller`.
- **OCP**: add a social provider (e.g. Apple) with a new package under
  `auth/common/adapter/out/external` containing a `SocialTokenVerifier`
  (returns `MemberRegisterCommand`; plus `SocialUnlinkClient` if needed) and a `Provider` enum value;
  `OAuthService` picks it up automatically. Do not modify existing providers.
- **Cross-domain cleanup** goes through member events; `member` never calls other domains' services
  (no package cycles). `MemberSuspendedEvent` revokes sessions; `MemberProfileImageChangedEvent` lets `upload`
  verify the new profile image (exception rolls the change back) and delete the previous file.
  - `MemberWithdrawnEvent` (soft delete): immediate actions (revoke tokens, social unlink, hide posts and wishes).
    Bulk updates in these listeners must not clear the persistence context (`clearAutomatically`), or the
    member change in the same transaction is lost. External API calls listen with
    `@TransactionalEventListener(phase = AFTER_COMMIT)` so they never hold a DB connection; they catch and log failures, and anything that must eventually succeed is retried
    from a `MemberPurgedEvent` listener (e.g. Kakao unlink).
  - `MemberPurgedEvent` (hard delete after 30 days): delete the member's data, including stored files.
    Listeners must be idempotent (a failure rolls back and retries).
- **LSP**: implementations honor the interface contract (same exceptions, no surprises).
- **DIP**: controllers and other services depend on `XxxUseCase` and ports, never on implementations.

### Service layer

- Each inbound operation has its own `port/in` interface (`GetMyProfileUseCase`, `CreatePostUseCase`), and
  callers inject only the use cases they call.
- A service implements the related use cases of one responsibility. Split it by cohesion when it grows
  (`PostCommandService` / `PostQueryService`; `MemberQueryService`, `MemberCommandService`,
  `MemberLifecycleService`), not into one class per use case.
- Introduce an abstraction only when multiple implementations exist or a client genuinely needs an
  isolated dependency (e.g. `SocialTokenVerifier`).
