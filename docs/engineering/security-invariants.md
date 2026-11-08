# Security and account invariants

These are invariants. Do not change them without being asked, and keep this document in sync with the code.

- JWT subject = member id. Access token 1h, refresh token 28d (`service-user` `application.yaml`). Only
  `service-user` holds the JWT secret and issues or parses tokens.
- Tokens are returned in the body and as `accessToken` / `refreshToken` cookies (HttpOnly, Secure,
  SameSite=None) via `JwtCookieManager`. Access token: `Authorization` header first, then cookie.
  Refresh: body first, then cookie.
- Gateway authentication: nginx sends every request (`auth_request`, GET without body, original headers) to
  `GET /internal/auth/verify` on `service-user`. The endpoint always answers 200: with `X-Member-Id` and
  `X-Member-Role` when the access token is valid, its session row exists and the member is active; without them
  otherwise. nginx copies those two headers onto the proxied request (`proxy_set_header` overwrites anything
  the client sent) and every service builds its authentication from them (`HeaderAuthenticationFilter`,
  `@AuthenticationPrincipal` = member id, `null` when the headers are absent). The services therefore trust the
  headers and must stay reachable only through nginx on the compose network (their ports are bound to the
  host's localhost for deploy checks only). The nginx internal listener (port 81) forwards only
  `/<service>/internal/**` and sends empty `X-Member-Id` / `X-Member-Role`, so the compose network cannot reach
  public or admin APIs as a member through it. The public listener blocks `/internal/` with `^~`, so a regex
  location added later cannot route it.
- Authorization per service: `/internal/**`, `/error` and the API docs are `permitAll` (nginx returns 404 for
  `/internal/**` and the API docs on the public listener); each service lists its public paths in its `SecurityConfig`;
  `/admin/**` requires `X-Member-Role: ADMIN`; everything else requires `X-Member-Id`.
- CSRF: cookie tokens are ignored when the request's `Origin` is not in `cors.allowed-origins`; a request from
  an origin outside that list is rejected by the CORS filter (403) before anything else, in every service.
- One session per account: every issue deletes the member's existing refresh tokens before saving the new one,
  and `refresh_token.member_id` is unique, so concurrent sign-ins leave one session (the loser gets 409).
- Session model: `refresh_token.session_id` = refresh `jti` = access `sid` claim. The gateway verification
  accepts an access token only while its session row exists (`SessionValidator`), so deleting refresh tokens
  (sign-out, login elsewhere, withdrawal) invalidates paired access tokens immediately, in every service.
- Refresh tokens are stored as SHA-256 hashes only and rotated on reissue (atomic delete). A token no longer
  stored is simply rejected (no revoke-all). Expired rows are purged daily by `RefreshTokenCleanupScheduler`.
- Sign-out (`/auth/logout`; all of `/auth/**` is permitted): when the gateway identified the member (valid
  access token), delete all the member's refresh tokens; otherwise delete the session of the refresh token from
  the body or cookie (so sign-out works after the access token expires; apps send it in the body, web sends
  the HttpOnly cookie). Expire both cookies and answer 204 whenever a token was sent, even if its session is
  already gone. With no token at all the caller is not signed in: 401 `INVALID_REFRESH_TOKEN` (same as refresh).
  Withdrawal also expires the cookies.
- Withdrawal (`DELETE /members/me`, 204) is a soft delete (`member.deleted_at`) that also clears
  `member.email`, revokes the sessions, and publishes `member.withdrawn`, which turns the member's posts
  `AUTHOR_WITHDRAWN` and hides their wishes from wish counts in `service-community`. The message is
  written to the outbox in the withdrawal transaction and sent after commit, so it leaves only if the withdrawal
  committed and a broker outage delays the hiding instead of failing the request. A withdrawn member cannot sign in or re-register with the same account for 30 days
  (`MEMBER_WITHDRAWN`); `getById` excludes withdrawn members (refresh is `INVALID_REFRESH_TOKEN`, not 404).
  `MemberPurgeScheduler` runs daily at 04:00, reads targets 100 at a time and hard-deletes one transaction per
  member: the social unlink listener runs first (`HIGHEST_PRECEDENCE`), then the service's own data (owned cars,
  interest cars), then `member.purged` is written to the outbox and the member row deleted in the same transaction. `service-community`
  deletes wishes, posts with parts and tags (publishing `post.deleted` per post), blocks and reports;
  `service-media` deletes every upload record and file of the member. Consumers are idempotent. Kakao unlink runs
  after the withdrawal commit and is retried before the purge if it fails; once the purge is 3 days overdue
  (`MemberPurgedEvent.finalAttempt`) the unlink failure is logged and the purge proceeds, so the 30-day deletion
  promise (Google Play policy) is kept.
- Member identity = `provider + provider_member_id` (unique). Email members use `provider = EMAIL`,
  `provider_member_id = email`; passwords are BCrypt hashes.
- Email sign-up: `send-code` (204) -> `verify-code` -> `signup`. 6-digit code bound to the email + purpose,
  valid 5 min, 5 wrong attempts invalidate it (`INVALID_VERIFICATION_CODE` with `remainingAttempts`), 60 s
  resend cooldown (`TOO_MANY_REQUESTS` with `retryAfterSeconds`; resend resets verification; the row is locked
  against concurrent resends). A correct code returns a `verificationToken` (10 min, single use, stored as a
  SHA-256 hash); sign-up consumes it in the same transaction that creates the member and signs in
  (`isNewMember = true`, 200). For an already registered (or withdrawn) email `send-code` answers the same
  204 and only a notice mail is sent (no account enumeration; the app says "메일이 안 오면 이미 가입된 이메일일
  수 있어요"); the code row is still created with a code nobody receives, so the resend cooldown and
  `verify-code` (`remainingAttempts`) behave exactly as for a new email. Every mail (code or notice) is sent on the
  application task executor after the response, so response time does not depend on which mail, if any, goes out;
  a failed send is logged and deletes the row so the user can request a new code.
- Sign-up requires the three consents (`agreements.age14/terms/privacy` all true) and a unique nickname
  (`member.nickname` is unique; `NICKNAME_ALREADY_EXISTS`). When a concurrent sign-up slips past the checks, the
  violated constraint decides between `NICKNAME_ALREADY_EXISTS` and `EMAIL_ALREADY_EXISTS`.
- Email sign-in errors are always `INVALID_CREDENTIALS`. 5 failures per email (registered or not) within
  15 min lock that email until the window ends (423 `ACCOUNT_LOCKED` with `retryAfterSeconds`); success or
  password reset clears the count. The lock duration is still an open question in the spec.
- Rate limits (`RateLimiter`, per service instance): sign-in 30 / 15 min per IP; sign-up and reset code sending
  10 / h per IP; code checks (sign-up and reset) 10 / day per email and purpose, so 6-digit codes cannot be
  brute-forced across IPs; upload URLs 100 / h per member. Unknown-email sign-in still runs a BCrypt compare.
- Password reset: `POST /auth/password/reset/code {email}` (204) -> `POST /auth/password/reset
  {email, code, newPassword}` (204). Same code rules as sign-up (purpose `RESET_PASSWORD`). For an email that
  is not an active email member the code API still answers 204 and sends nothing, but creates the code row all the
  same so the cooldown and the `remainingAttempts` of `/auth/password/reset` do not reveal whether it is
  registered (the mail, when there is one, also goes out after the response). The code check commits its attempt
  count on its own; a correct code is consumed, then the password change and deletion of all the member's sessions
  run in one transaction.
- Suspension: `PATCH /admin/reports/{id}` with `SUSPEND_MEMBER` makes `service-community` call
  `POST /internal/members/{id}/suspend`; `service-user` sets `member.suspended_until` and deletes the member's
  sessions; `TokenService.issue` rejects suspended members (`MEMBER_SUSPENDED`), covering every sign-in and refresh.
- Admin: the gateway verification reads the member's current role from the DB on every request, not the
  token's `role` claim, so `/admin/**` reflects a role change without a new login. Admins sign up normally and
  get `role = ADMIN` set in the DB. Report processing records who (`resolved_by`), when and what. Hiding an
  already deleted post or suspending an already withdrawn member is a no-op, so such reports can still be
  closed with that action.
- Public endpoints: all of `/auth/**` (service-user); GET `/car-models/**` (service-car); GET `/feed`,
  `/posts/{id}`, `/members/{id}/posts` (service-community); with DB storage also GET `/images/**` and
  PUT `/uploads/files/**` (service-media, authenticated by the upload URL signature); the Swagger UI and
  `/v3/api-docs/**` of each service (in `prod` reachable only through an SSH tunnel to the service's
  localhost port: the public nginx listener answers 404 for `/swagger-ui*` and `/v3/api-docs*`); `/error`. Every other path requires a member
  identified by the gateway. When authenticated, feed, post detail and member post lists also exclude posts of
  members in a block relation (either direction).
- Uploads: the app crops, resizes, strips location data, masks plates and converts to JPEG/WebP, then PUTs
  straight to storage; the server never receives the file in prod. `POST /uploads/images` (service-media)
  returns per image a temporary `imageKey` (`tmp/posts/...`, `tmp/profiles/...`) and a 10-min presigned
  `uploadUrl` with the Content-Type (`image/jpeg` or `image/webp`) bound into the signature. When a post
  (service-community) or profile (service-user) uses the key, it calls `POST /internal/images/attach`, which checks
  that the key belongs to that member and purpose and that the stored object is valid (HEAD: <= 10 MB, same
  Content-Type; first bytes: real JPEG/WebP), copies it to the permanent key without `tmp/` and deletes the
  temporary one (`INVALID_IMAGE` otherwise, propagated unchanged to the caller). Storage side effects cannot roll
  back with the DB: media copies in its transaction and deletes the temporary file after commit; the caller
  deletes the attached images again (`POST /internal/images/delete`) when its own transaction rolls back; every
  other file deletion (post delete via `post.deleted`, profile change after commit, purge via `member.purged`)
  happens after the owning transaction committed. Unattached uploads are deleted after 1 day (server rows + R2
  lifecycle rule on `tmp/`). DB storage (local/test) signs its own URL with HMAC-SHA256 (`storage.db.secret`).
- Posts reference only the member's own owned car (`FORBIDDEN` from `GET /internal/owned-cars/{id}` otherwise);
  wishes reference `postPartId`, are removed with the post or part, and the author's own wishes are not counted.
- Passwords: 8-64 chars, at least one letter and one digit, at most 72 UTF-8 bytes (`@MaxUtf8Bytes(72)`).
- Social emails are stored only when the provider marks them verified; `member.email` is nullable.
  Social sign-up never uses the provider's name (Google `name` is a real name); the nickname is always
  generated as `이지리오너` + 6 digits (unique) and can be changed later.
- Kakao API: only 400/401 mean an invalid provider token (`INVALID_PROVIDER_TOKEN`); other errors are
  `SOCIAL_SERVER_ERROR`. `APPLE` exists as a provider value but has no verifier yet (`UNSUPPORTED_PROVIDER`).
- Secrets come from `.env` (never commit it). Never log passwords, tokens, or secrets.
