# IjiriServer agent guide

Spring Boot 3.5 / Java 21 / Gradle / PostgreSQL / Spring Data JPA / Spring Security (stateless JWT).
Members sign up with email + password (SMTP email verification) or sign in with Kakao/Google
SDK tokens, which the server verifies before issuing its own JWTs.

## Working rules

- Make the smallest change that fully addresses the request. Follow existing code when sound; do not
  refactor unrelated code.
- Before changing code, inspect the relevant implementation, tests, configuration, and applicable
  documents under `docs/engineering/`. For cross-cutting changes, inspect all affected areas.
- Re-read every changed file and check correctness, SOLID, conventions, line length, and leftovers
  (unused imports, dead code, stale TODOs). Fix what you find.
- Run the narrowest relevant checks. For behavior changes or startup-impacting changes, run
  `./gradlew test`; documentation-only changes need no tests. Report exact errors and never claim
  checks passed when they did not.
- Do not narrate routine steps. Give concise progress updates only when work is substantial.
- Act on clear, rule-conforming fixes. Ask only when a decision affects product behavior or requirements
  are genuinely ambiguous.
- Final report in Korean, concise, no emoji: what changed, why, and which checks ran.
- Do not commit unless explicitly asked. For multi-file or multi-concern changes, propose a commit plan
  (see `docs/engineering/commit-convention.md`).
- Use available tools and skills (docs lookup, code review, security review) when they materially help.
- When changing behavior described by the engineering documents, update the relevant document in the
  same change, especially `docs/engineering/security-invariants.md`.

## Engineering documents

Read the relevant guide before editing that area:

- `docs/engineering/architecture.md`: project structure, package ownership, domain boundaries, design.
- `docs/engineering/coding-conventions.md`: naming, Java style, entities, schema, and API rules.
- `docs/engineering/security-invariants.md`: authentication, authorization, token and account lifecycle.
- `docs/engineering/operations.md`: profiles, test setup, commands, deploy, CI/CD, and operational limitations.
- `docs/engineering/commit-convention.md`: commit message and commit planning rules.

Keep these documents focused. When code changes, verify the documented behavior against the source.
Do not assume listed classes or endpoints are current without checking the source tree.
