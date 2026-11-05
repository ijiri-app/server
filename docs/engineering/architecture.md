# Architecture

## Modules and services

Gradle multi-module build. `common` is a library; each `service-*` module is its own Spring Boot application with
its own PostgreSQL database, Flyway migrations, container image and blue/green slot. Keep this layout; a new
feature goes into the service that owns its data. Check the source tree before relying on specific classes or
endpoints; do not create empty packages.

```
IjiriServer
├── common                        ijiri.ijiriserver.common (library, no main class)
│   ├── config                    ClockConfig (Asia/Seoul Clock, JPA auditing, @EnableScheduling),
│   │                             HttpClientConfig (external and internal request factories), JacksonConfig,
│   │                             SwaggerConfig (per-service docs, X-Member-Id scheme)
│   ├── contract                  DTOs and enums every service agrees on: BuildStyle, PartCategory, UploadPurpose,
│   │                             CarSpec, CarModelInfo, PartInfo, PartResolveRequest, OwnedCarSnapshot,
│   │                             MemberSummary, MemberCounts, SuspendMemberRequest, AttachImagesRequest,
│   │                             AttachedImage, DeleteImagesRequest
│   ├── entity                    BaseTimeEntity
│   ├── exception                 CustomException, StatusCode, CommonStatusCode, RemoteStatusCode;
│   │                             handler/: GlobalExceptionHandler, CustomErrorController, 401/403 handlers
│   ├── internal                  InternalClient, InternalClientFactory (REST calls between services)
│   ├── jwt                       JwtProvider, JwtCookieManager, SessionValidator (beans only in service-user)
│   ├── messaging                 Topics, Kafka message classes, MessagePublisher (KafkaMessagePublisher)
│   ├── ratelimit                 RateLimiter (in-memory fixed window)
│   ├── response                  BaseResponse
│   ├── security                  GatewayHeaders, HeaderAuthenticationFilter, ServiceSecurityConfig
│   ├── storage                   ImageUrlResolver (bean only in service-media)
│   ├── transaction               TransactionHooks (afterCommit / afterRollback)
│   └── validation                @MaxUtf8Bytes
├── service-user                  ijiri.ijiriserver.user   DB ijiri_user
│   ├── auth/common               SocialTokenVerifier, SocialUnlinkClient, kakao/google clients, AuthResponse
│   ├── auth/email                POST /auth/signup, /auth/login/email, /auth/logout, /auth/password/reset/code,
│   │                             /auth/password/reset, /auth/email/send-code, /auth/email/verify-code
│   ├── auth/oauth                POST /auth/login (provider + token)
│   ├── auth/token                POST /auth/refresh; GET /internal/auth/verify (gateway, AuthVerifyController)
│   ├── member                    GET/PATCH/DELETE /members/me, GET /members/{id};
│   │                             GET /internal/members, /internal/members/{id}, POST /internal/members/{id}/suspend
│   ├── ownedcar                  GET/POST /members/me/cars, PATCH/DELETE /members/me/cars/{id};
│   │                             GET /internal/owned-cars/{id}?memberId=
│   └── interestcar               GET/PUT /members/me/interest-cars; GET /internal/members/{id}/interest-cars
├── service-community             ijiri.ijiriserver.community   DB ijiri_community
│   ├── post                      POST /posts, GET/PATCH/DELETE /posts/{id}, GET /feed, GET /members/{id}/posts;
│   │                             GET /internal/members/{id}/counts, /internal/owned-cars/post-counts
│   ├── wishlist                  POST /wishlist, DELETE /wishlist/{id}, GET /members/me/wishlist
│   ├── block                     POST/DELETE /members/{id}/block, GET /members/me/blocks
│   ├── report                    POST /reports; GET /admin/reports, PATCH /admin/reports/{id}
│   └── messaging                 MemberEventConsumer (member.withdrawn, member.purged)
├── service-car                   ijiri.ijiriserver.car   DB ijiri_car
│   ├── carmodel                  GET /car-models, /car-models/{id}; GET /internal/car-trims, /internal/car-trims/{id},
│   │                             /internal/car-models; master imported from resources/car-master/car_master.csv
│   │                             at startup (insert-only)
│   └── part                      GET /parts, /parts/suggest, /brands; POST /internal/parts/resolve,
│                                 GET /internal/parts; PartUsageConsumer (part.usage-changed)
└── service-media                 ijiri.ijiriserver.media   DB ijiri_media
    └── upload                    POST /uploads/images; POST /internal/images/attach, /internal/images/delete;
                                  ImageStorageClient (R2ImageStorageClient for prod, DbImageStorageClient for
                                  local/test with ImageFileController: PUT /uploads/files/**, GET /images/**);
                                  UploadEventConsumer (post.deleted, member.purged); UploadedImageCleanupScheduler
```

Each service keeps the hexagonal layout inside its features:

```
<service>/<feature>
├── adapter
│   ├── in/web/controller      HTTP; XxxInternalController for /internal endpoints (@Hidden)
│   ├── in/messaging           Kafka consumers (XxxConsumer); a consumer spanning several features lives in
│   │                          <service>/messaging
│   ├── in/scheduler           scheduled jobs
│   ├── out/persistence        XxxJpaRepository, XxxPersistenceAdapter, providers for other features' ports
│   ├── out/external           external APIs, mail, storage and other services' /internal APIs (XxxServiceClient)
│   └── out/messaging          Kafka publishers (KafkaXxxEventPublisher)
├── application
│   ├── data                   commands and results; data/request, data/response
│   ├── port/in                XxxUseCase
│   ├── port/out               XxxPort, reader and publisher interfaces
│   └── service                XxxService (implements XxxUseCase)
├── entity                     JPA entities, enums and value types owned by this service
├── event                      Spring events inside the service
└── exception
```

- Dependencies point inward: `application` never imports `adapter`; `entity`, `event` and `exception` import
  neither `application` nor `adapter`. Controllers and other features depend on `port/in` use cases.
- Services use outbound ports, never `XxxJpaRepository`, a concrete client or `InternalClient` directly. A port
  declares only the methods in use. JPA entities double as the domain model.
- A service never imports another service's module. Data of another service arrives as a `common/contract`
  type through a `port/out` reader implemented by an `adapter/out/external` client, or as a Kafka message.
- `common` contains only cross-cutting infrastructure and the contracts, never business logic.

## Service boundaries

| Service | Owns | Calls (REST, sync) | Publishes | Consumes |
|---|---|---|---|---|
| service-user | member, session, email verification, owned car, interest car | car (trim spec, car model infos), community (post and wish counts), media (attach and delete profile image) | member.withdrawn, member.purged | - |
| service-community | post, wishlist, block, report | user (member summaries, owned car snapshot, interest cars, suspend), car (trim spec, model names, part resolve and infos), media (attach and delete post images) | post.deleted, part.usage-changed | member.withdrawn, member.purged |
| service-car | car model, generation, trim, brand, part, alias, usage | - | - | part.usage-changed |
| service-media | uploaded image, image file (DB storage) | - | - | post.deleted, member.purged |

Only id columns cross a service boundary; there are no foreign keys or joins between services. Inside the `post`
aggregate, `post_image`, `post_part` and `post_part_tag` use JPA relations and FKs.

### Internal REST

Every `/internal/**` endpoint answers with `BaseResponse` like the public API, is `permitAll` in the service and
is not routed by the public nginx listener (404). Services reach each other through the nginx internal listener
(`http://nginx:81/<service>/internal/...`) so blue/green switches apply to internal calls too. `InternalClient`
turns an error response into a `CustomException` with a `RemoteStatusCode` carrying the upstream HTTP status, code
and message, so a caller answers with the same error (`INVALID_TRIM`, `INVALID_IMAGE`, `FORBIDDEN`, `NOT_FOUND`).
Connection failures and 5xx become `SERVICE_UNAVAILABLE` (502).

### Events (Kafka)

Messages are JSON strings keyed by the aggregate id. One listener per topic per service (the consumer group is
the service name), so messages of one member or post are consumed in order. Consumers must be idempotent: a
message is delivered at least once, and a failing listener is retried by Spring Kafka's default error handler
and then skipped with a log.

- `member.withdrawn` (`MemberWithdrawnMessage`): community hides the member's posts and wishes.
- `member.purged` (`MemberPurgedMessage`): community deletes wishes, posts (publishing `post.deleted` per post),
  blocks and reports; media deletes every upload record and file of the member.
- `post.deleted` (`PostDeletedMessage` with image keys): media deletes the images.
- `part.usage-changed` (`PartUsageChangedMessage`): car adjusts total and per-car-model use counts.

`service-user` publishes synchronously inside the withdrawal and purge transactions (the broker must acknowledge),
so a broker outage rolls the operation back and the purge is retried by the next schedule. `service-community`
publishes after commit (`TransactionHooks.afterCommit`), so a lost `post.deleted` leaves orphan files and a lost
`part.usage-changed` drifts a ranking counter; both are logged.

## Design principles

- **SRP**: external calls in `adapter/out`, orchestration in `application/service`, HTTP mapping in `controller`.
- **OCP**: add a social provider (e.g. Apple) with a new package under `service-user` `auth/common/adapter/out/external`
  containing a `SocialTokenVerifier` (plus `SocialUnlinkClient` if needed) and a `Provider` enum value;
  `OAuthService` picks it up automatically.
- **Cross-feature cleanup inside a service** goes through Spring events (`MemberWithdrawnEvent`, `MemberPurgedEvent`,
  `MemberSuspendedEvent` in service-user; `PostDeletedEvent`, `PostPartsRemovedEvent` in service-community) in the
  same transaction; cross-service cleanup goes through Kafka. External API calls that must not hold a DB connection
  listen with `@TransactionalEventListener(phase = AFTER_COMMIT)` (social unlink after withdrawal).
- **Storage side effects** cannot roll back with the DB: media copies a file inside the transaction and deletes the
  temporary key after commit; a caller that attached images registers `TransactionHooks.afterRollback` to delete
  them again when its own transaction fails.
- **DIP**: controllers and services depend on `XxxUseCase` and ports, never on implementations or clients.

### Service layer

- Each inbound operation has its own `port/in` interface, and callers inject only the use cases they call.
- A service implements the related use cases of one responsibility and is split by cohesion when it grows
  (`PostCommandService` / `PostQueryService`; `MemberQueryService`, `MemberCommandService`, `MemberLifecycleService`).
- Introduce an abstraction only when multiple implementations exist or a client genuinely needs an isolated
  dependency (`SocialTokenVerifier`, `ImageStorageClient`, the cross-service readers).
