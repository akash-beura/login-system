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

---

### 2026-03-06 — Hardcoded Secure cookie flag breaks local dev
**Symptom**: Page refresh logs the user out; refresh token cookie silently dropped by browser
**Root cause**: `jakarta.servlet.http.Cookie` with `setSecure(true)` hardcoded. Browsers reject `Secure` cookies over plain HTTP (localhost dev). Additionally, `jakarta.servlet.http.Cookie` has no native `SameSite` support, forcing fragile `Set-Cookie` header string manipulation.
**Fix**: Switched to `org.springframework.http.ResponseCookie` (has native `sameSite()` builder). Made `Secure` flag config-driven via `app.cookie.secure` (true in prod, false in dev profile).
**Prevention rule**: Never hardcode `Secure=true` on cookies — always drive it from config. Use `ResponseCookie` (not `jakarta.servlet.http.Cookie`) for full cookie attribute control including SameSite.

---

### 2026-03-06 — React useEffect dependency on state it sets causes re-fire
**Symptom**: Auth restore effect re-ran after setting accessToken, triggering unnecessary /auth/refresh calls
**Root cause**: `useEffect(..., [accessToken])` — the effect sets `accessToken` via `setAccessToken`, which changes the dep, re-triggering the effect
**Fix**: Changed dependency to `[]` (mount-only). Added eslint-disable comment with reason.
**Prevention rule**: Never include state that the effect itself sets in its dependency array — this creates a re-fire loop. Mount-only effects use `[]`.

---

### 2026-03-06 — Custom header not added to CORS allowedHeaders
**Symptom**: All authenticated cross-origin requests fail silently; browser blocks preflight
**Root cause**: Added `X-Session-Token` custom header to frontend requests but did not add it to `CorsConfig.setAllowedHeaders()`. Browsers reject custom headers not listed in `Access-Control-Allow-Headers`.
**Fix**: Add every custom request header to `CorsConfig.setAllowedHeaders()` when introducing it
**Prevention rule**: When adding any custom HTTP header to frontend requests, immediately add it to the CORS `allowedHeaders` list — preflight will block it otherwise.

---

### 2026-03-06 — Session token not bound to user identity in filter
**Symptom**: Any valid session token passes validation regardless of which user it belongs to
**Root cause**: `JwtAuthFilter` validates session token exists in Redis but never checks that the session's userId matches the JWT's userId. Two independent valid credentials are checked in isolation.
**Fix**: Compare `sessionUserId` from Redis with `userId` from JWT; reject if mismatched
**Prevention rule**: When validating multiple credentials in a filter (JWT + session), always cross-check that they belong to the same principal — never validate them independently.

---

### 2026-03-06 — Stale env vars left in docker-compose after config migration
**Symptom**: docker-compose.yml passes `JWT_REFRESH_EXPIRY_MS` that no config property binds to
**Root cause**: Removed `refresh-expiry-ms` from `application.yml` and `AppProperties` but forgot to clean up docker-compose env vars
**Fix**: Remove stale env var, add new `SESSION_TIMEOUT_MINUTES` if needed
**Prevention rule**: When removing a config property, grep all deployment files (docker-compose, .env.example, CI configs) for the corresponding env var and remove it too.
