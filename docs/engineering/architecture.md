# Architecture

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
│   │   ├── email                 POST /auth/signup, /auth/login/email, /auth/logout,
│   │   │   │                     /auth/password/reset/code, /auth/password/reset,
│   │   │   │                     /auth/email/send-code, /auth/email/verify-code
│   │   │   │                     (AuthController, EmailVerificationController)
│   │   │   ├── client  controller  dto/request  entity  repository
│   │   │   ├── scheduler  service  service/impl
│   │   ├── oauth                 POST /auth/login (provider + token), OAuthController
│   │   │   ├── controller  dto/request  service  service/impl
│   │   ├── kakao                 client only: KakaoOAuthClient (verify), KakaoUnlinkClient
│   │   ├── google                client only: GoogleOAuthClient (verify)
│   │   └── token                 POST /auth/refresh (TokenController)
│   │       ├── controller  dto/request  entity
│   │       ├── repository  scheduler  service  service/impl
│   ├── member                    GET/PATCH/DELETE /members/me, GET /members/{id}
│   │   ├── controller  dto  dto/request  dto/response  entity  event  exception
│   │   ├── repository  scheduler  service  service/impl
│   ├── interestcar               GET/PUT /members/me/interest-cars
│   │   ├── controller  dto/request  dto/response  entity  exception
│   │   ├── repository  service  service/impl
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
    ├── exception                 StatusCode, CommonStatusCode, CustomException, GlobalExceptionHandler,
    │                             CustomErrorController (/error -> BaseResponse)
    ├── jwt                       JwtProvider, JwtAuthenticationFilter, JwtCookieManager, SessionValidator
    ├── ratelimit                 RateLimiter (in-memory fixed window)
    ├── response                  BaseResponse
    ├── security                  401/403 handlers returning BaseResponse, AdminVerifier
    ├── storage                   ImageUrlResolver (image key <-> public URL)
    └── validation                @MaxUtf8Bytes
```

Resources: `db/migration` holds the Flyway migrations (`V{n}__description.sql`), `car-master/car_master.csv`
the car master data.

Domain dependencies (no cycles): `post` -> `ownedcar`, `carmodel`, `interestcar`, `part`, `upload`, `block`,
`member`; `wishlist` -> `post`, `member`; `report` -> `post`, `member`; `ownedcar`/`interestcar` -> `carmodel`,
`member`. `upload`, `part`, `carmodel` depend on no other domain service. When a lower domain needs data from a
higher one, it declares a small reader interface that the higher domain implements with a repository-only
component (`MemberPostCounter`, `MemberWishCounter`, `OwnedCarPostCounter`, `PostWishReader`, implemented by
`PostCountProvider` and `WishCountProvider`); higher domains notify lower ones with events
(`PostDeletedEvent`, `PostPartsRemovedEvent`). Shared value types used by several domains
(e.g. `BuildStyle`) live in the lowest domain (`carmodel`). Within the `post` aggregate, `post_image` and
`post_part_tag` use JPA relations and FKs; across domains there are only id columns.

## Package ownership

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
