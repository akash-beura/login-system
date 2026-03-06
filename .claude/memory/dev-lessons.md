# Dev Lessons — Anti-Pattern Registry

Captures critical errors, loops, and anti-patterns encountered by dev agents.
Read this file BEFORE starting any implementation task to avoid repeating mistakes.

## Format
```
### [Date] - [Short title]
**Symptom**: What went wrong (error, loop, wrong output)
**Root cause**: Why it happened
**Fix**: What resolved it
**Prevention rule**: One-liner rule to never repeat this
```

---

### 2026-03-05 — Stale JAR deployed silently via Docker
**Symptom**: Docker container ran old code despite code changes
**Root cause**: Dockerfile copies JAR from local `target/` — stale build artifact
**Fix**: Run `mvn clean install -DskipTests` before `docker compose up --build`
**Prevention rule**: Never trust local `target/` — always rebuild before Docker build, or use multi-stage Docker builds

---

### 2026-03-05 — ARM64 Docker image crash
**Symptom**: Container crashed on Apple Silicon / ARM64
**Root cause**: Used `-alpine` JRE image which lacks ARM64 support for some packages
**Fix**: Use `eclipse-temurin:17-jre` (Debian-based) instead of `-alpine`
**Prevention rule**: Always use Debian-based JRE images for ARM64 compatibility

---

### 2026-03-05 — npm install failures
**Symptom**: `npm ci` fails with peer dependency conflicts
**Root cause**: React ecosystem has widespread peer dep version mismatches
**Fix**: Use `npm install --legacy-peer-deps`
**Prevention rule**: Always use `--legacy-peer-deps` flag in this project

---

### 2026-03-05 — OAuth2 session conflict with STATELESS policy
**Symptom**: OAuth2 login fails with missing state parameter
**Root cause**: Spring OAuth2 needs a session to store the state parameter, but STATELESS disables sessions
**Fix**: Keep `SessionCreationPolicy.STATELESS` but don't fully disable sessions — Spring handles this correctly
**Prevention rule**: Never disable sessions when OAuth2 is active — the state parameter requires it
