# IjiriServer agent guide

Spring Boot 3.5 / Kotlin 2.4 on Java 21 / Gradle multi-module / PostgreSQL (one per service) / Spring Data JPA /
Spring Security / Kafka. Four Spring Boot services behind an nginx gateway: `service-user` (auth, member, owned
car, interest car), `service-community` (post, feed, wishlist, block, report), `service-car` (car master, part,
brand), `service-media` (image upload and storage). `common` is a shared library (config, security, exception,
response, cross-service contracts, Kafka messages).
Members sign up with email + password (SMTP email verification) or sign in with Kakao/Google SDK tokens, which
`service-user` verifies before issuing its own JWTs. nginx asks `service-user` to verify every request and
forwards the result to the services as `X-Member-Id` / `X-Member-Role`.

## Working rules

- Make the smallest change that fully addresses the request. Follow existing code when sound; do not
  refactor unrelated code.
- Before changing code, inspect the relevant implementation, tests, configuration, and applicable
  documents under `docs/engineering/`. For cross-cutting changes, inspect all affected areas and modules.
- Re-read every changed file and check correctness, SOLID, conventions, line length, and leftovers
  (unused imports, dead code, stale TODOs). Fix what you find.
- Run the narrowest relevant checks: `./gradlew :service-user:test` for one module, `./gradlew test` for
  behavior or startup-impacting changes that touch `common` or several modules; documentation-only changes
  need no tests. Report exact errors and never claim checks passed when they did not.
- Do not narrate routine steps. Give concise progress updates only when work is substantial.
- Act on clear, rule-conforming fixes. Ask only when a decision affects product behavior or requirements
  are genuinely ambiguous.
- Final report in Korean, concise, no emoji: what changed, why, and which checks ran.
- Do not commit unless explicitly asked. For multi-file or multi-concern changes, propose a commit plan
  (see `docs/engineering/commit-convention.md`).
- Use available tools and skills (docs lookup, code review, security review) when they materially help.
- When changing behavior described by the engineering documents, update the relevant document in the
  same change, especially `docs/engineering/security-invariants.md`.
- A change to a cross-service contract (`common/contract`, `common/messaging`, an `/internal` endpoint) touches
  both the provider and every consumer; update them in the same change and deploy them together.

## Engineering documents

Read the relevant guide before editing that area:

- `docs/engineering/architecture.md`: modules, services, package ownership, domain boundaries, contracts, events.
- `docs/engineering/coding-conventions.md`: naming, Kotlin style, entities, schema, API and internal API rules.
- `docs/engineering/security-invariants.md`: gateway authentication, authorization, token and account lifecycle.
- `docs/engineering/operations.md`: profiles, test setup, commands, deploy, CI/CD, and operational limitations.
- `docs/engineering/commit-convention.md`: commit message and commit planning rules.

Keep these documents focused. When code changes, verify the documented behavior against the source.
Do not assume listed classes or endpoints are current without checking the source tree.
