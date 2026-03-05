# Lessons Learned

Captures patterns from mistakes, corrections, and debugging sessions.
Update this file after any user correction or unexpected failure.

## Format
```
### [Date] - [Short description]
**What happened**: ...
**Root cause**: ...
**Rule going forward**: ...
```

---

## ARM64 Docker
**Rule**: Use `eclipse-temurin:17-jre` (not `-alpine`) for ARM64. Use `maven:3.9-eclipse-temurin-17` for builds.
Use `groupadd -r spring && useradd -r -g spring spring` (Debian syntax, not Alpine adduser).

## Stale JAR in Docker
**Rule**: Always run `mvn clean install -DskipTests` before `docker compose up --build`.
The Docker build copies the JAR from `target/` — a stale JAR will deploy old code silently.

## npm install peer deps
**Rule**: Use `npm install --legacy-peer-deps` for this project. `npm ci` fails due to peer dependency conflicts.

## Spring OAuth2 + STATELESS
**Rule**: Spring OAuth2 requires a session for the OAuth2 state parameter even with `SessionCreationPolicy.STATELESS`.
Do not disable sessions completely when OAuth2 is in use — configure them to `STATELESS` but allow Spring to create session only for OAuth2 state management.

## ddl-auto in prod
**Rule**: `application-prod.yml` sets `ddl-auto: validate`. Docker Compose overrides via environment variable to `update` for dev convenience.
Never set `ddl-auto: create-drop` or `create` in prod profile.

## Focus ring tokens
**Rule**: All form inputs use `var(--focus-ring)` CSS custom property. Never hardcode rgba focus colors in components.
