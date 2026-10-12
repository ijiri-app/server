# Commit convention

- Commit messages are always in English.
- Format: `<type>: <summary>`, imperative mood, no scope, no trailing period, max 72 chars.
- Types: `feat`, `fix`, `refactor`, `chore` (build/config), `docs`, `test`, `style`.
- One commit represents one logical change. Group files by change, not by file type.
- Do not commit unless explicitly asked.
- For multi-file or multi-concern changes, propose a commit plan after the report. Use ready-to-paste
  commands, one `git add -A <paths>` + `git commit` pair per commit, ordered so each commit compiles.
  Keep each command on one line.

```bash
git add -A src/main/java/ijiri/ijiriserver/domain/auth/token
git commit -m "refactor: move RefreshTokenServiceImpl to service/impl"
```
