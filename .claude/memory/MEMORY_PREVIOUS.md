# Session Memory — Login System (Post-Implementation)

This file captures the full implementation state after two working sessions.
Use this as the source of truth when resuming work.

---

## Project Status
**Fully implemented and running via Docker Compose.**
All core auth flows working: register, login, Google OAuth, account linking, set-password, logout.

---

## Tech Stack
- **Backend**: Spring Boot 3.2.5, Java 17, Spring Security, Spring OAuth2 Client, JPA/Hibernate, PostgreSQL 16
- **Frontend**: React 18 (CRA), React Router v6, Axios, CSS Modules
- **Infrastructure**: Docker Compose (postgres + backend + frontend), nginx for frontend

---

## Backend — Key Architecture

### Package: `com.akash.loginsystem`
| Layer | Path |
|---|---|
| Entity | `entity/User.java`, `entity/RefreshToken.java` |
| DTO | `dto/request/`, `dto/response/` |
| Service | `service/AuthService.java`, `service/impl/AuthServiceImpl.java` |
| Controller | `controller/AuthController.java` |
| Security | `security/JwtProvider`, `JwtAuthFilter`, `CustomUserDetailsService` |
| OAuth2 | `security/oauth2/OAuth2UserService`, `OAuth2SuccessHandler`, `OAuthTokenStore` |
| Config | `config/SecurityConfig`, `AuthBeanConfig`, `AppProperties`, `CorsConfig` |
| Exception | `GlobalExceptionHandler`, custom exceptions |

### User Entity Fields (current)
```
id (UUID), email, password (nullable), name,
provider (LOCAL/GOOGLE), providerId,
passwordSet (boolean, default false),
role (USER/ADMIN),
phoneCountryCode, phoneNumber,
addressLine1, city, state, zipCode, country,
createdAt, updatedAt
```

### Critical Fix: Circular Dependency
`AuthBeanConfig.java` was created to extract `PasswordEncoder`, `DaoAuthenticationProvider`,
and `AuthenticationManager` out of `SecurityConfig` to break a circular dep:
`SecurityConfig → OAuth2SuccessHandler → AuthServiceImpl → PasswordEncoder → SecurityConfig`

### Critical Fix: OAuth requiresPasswordSet
`AuthServiceImpl.issueTokens()` — was hardcoded `requiresPasswordSet(false)`.
Fixed to: `requiresPasswordSet(!user.isPasswordSet())`

### API Endpoints (`/api/v1/auth/`)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | — | Register with all fields (7 optional: phone, address) |
| POST | `/login` | — | Login; returns `requiresPasswordSet: true` if OAuth-only account |
| POST | `/refresh` | — | Rotate refresh token |
| POST | `/set-password` | Bearer | Set password for OAuth users after account linking |
| POST | `/oauth2/token` | — | Exchange one-time opaque code for tokens |
| GET | `/actuator/health` | — | Health check |

### RegisterRequest — Optional Fields Added
`phoneCountryCode`, `phoneNumber`, `addressLine1`, `city`, `state`, `zipCode`, `country`
All nullable in DB, no validation annotations, mapped in `User.builder()` inside `register()`.

---

## Frontend — Key Architecture

### Pages
| Page | Route | Notes |
|---|---|---|
| `LoginPage` | `/login` | Facebook two-column layout (branding left, form right on ≥820px) |
| `RegisterPage` | `/register` | Wide card (560px), phone+address fields, "It's quick and easy." |
| `SetPasswordPage` | `/set-password` | Protected; requires token in context |
| `SetPasswordPromptPage` | `/set-password-prompt` | Tells OAuth user to re-auth via Google first |
| `OAuthCallbackPage` | `/oauth/callback` | Exchanges one-time code → tokens → navigates |
| `HomePage` | `/homepage` | Protected; shows user info + logout |

### Auth State
- `AuthContext` — in-memory only (React state). Never localStorage. Token lost on refresh (by design).
- `useAuth()` hook exposes `{ login, logout, accessToken, user, isAuthenticated }`

### Theme System
- `ThemeContext` — defaults to `'dark'`, persists to `localStorage`
- `data-theme="dark"` applied to `<html>` element
- `ThemeToggle` component — fixed bottom-right, sun/moon icon
- Both `ThemeProvider` and `ThemeToggle` wired in `App.jsx` (outside BrowserRouter)

### Design Tokens (`styles/tokens.css`)
**Light mode:**
- `--bg-app: #f0f2f5` (Facebook gray)
- `--bg-card: #ffffff`
- `--color-primary: #1877f2` (Facebook blue)
- `--input-bg: #ffffff`
- `--border-color: #ccd0d5`
- `--focus-ring: rgba(24, 119, 242, 0.2)`

**Dark mode (`[data-theme="dark"]`):**
- `--bg-app: #18191a`
- `--bg-card: #242526`
- `--color-primary: #2d88ff`
- `--input-bg: #3a3b3c`
- `--border-color: #3e4042`
- `--focus-ring: rgba(45, 136, 255, 0.25)`

### CSS Module Conventions
- All component styles use CSS Modules (`.module.css`)
- All colors/spacing via CSS custom properties from `tokens.css`
- `AuthLayout` accepts `cardClassName` and `pageClassName` props for per-page overrides
- `RegisterPage` uses `regStyles.wideCard` (560px) + `regStyles.scrollPage` (top-aligned)
- `LoginPage` does NOT use `AuthLayout` — has its own two-column layout module

### authService.js — Register Signature Change
`register(formData)` now accepts a single object (not 3 positional args).
Spreads entire form (including optional fields) directly to `POST /auth/register`.

---

## Docker Compose

### Services
| Container | Image | Port |
|---|---|---|
| `loginsystem-postgres` | postgres:16-alpine | 5432 |
| `loginsystem-backend` | eclipse-temurin:17-jre (pre-built JAR) | 8080 |
| `loginsystem-frontend` | node:20-alpine → nginx:1.27-alpine | 3000 |

### Key Config
- Backend Dockerfile copies `target/*.jar` — **must run `mvn clean install -DskipTests` first**
- `SPRING_JPA_HIBERNATE_DDL_AUTO: update` set in docker-compose (overrides prod `validate`)
- Networks: `internal` (postgres↔backend only), `external` (backend↔frontend)

### To Start
```bash
# After backend code changes:
cd login-system-backend && mvn clean install -DskipTests && cd ..
docker compose up --build

# No backend changes:
docker compose up --build   # frontend only rebuild
docker compose up           # no rebuilds needed
```

### URLs
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Health: http://localhost:8080/actuator/health

---

## Docs Generated
| File | Description |
|---|---|
| `docs/running-and-testing.md` | Setup + start/stop + curl examples |
| `docs/openapi.yaml` | OpenAPI 3.0.3 full spec |
| `docs/postman-collection.json` | Postman v2.1 importable collection |

---

## Known Patterns & Gotchas
1. **Stale JAR in Docker**: If backend changes don't appear, run `mvn clean install -DskipTests` then `docker compose up --build`
2. **ARM64 Docker**: Use `eclipse-temurin:17-jre` (not `-alpine`) and `maven:3.9-eclipse-temurin-17` — Alpine variants lack ARM64 support
3. **Debian vs Alpine adduser**: Use `groupadd -r spring && useradd -r -g spring spring` (not `addgroup -S`)
4. **npm install**: Use `--legacy-peer-deps` (not `npm ci`) — package-lock.json may have peer dep conflicts
5. **SessionCreationPolicy.STATELESS + OAuth2**: Spring OAuth2 client needs session for state param. Watched for but hasn't caused issues yet.
6. **`ddl-auto: validate`** in `application-prod.yml` — docker-compose overrides to `update` via env var. Do NOT set in yaml.
7. **Focus ring token**: All select/input focus shadows use `var(--focus-ring)` — no hardcoded rgba values
