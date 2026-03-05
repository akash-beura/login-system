# Architecture — Login System (Current State)

> Original scaffold: see [project_skeleton.md](./project_skeleton.md)
> Last updated: 2026-03-05

---

## System Overview

Full-stack authentication system with Google OAuth2 integration.

```
Browser
  └── React App (port 3000)
        ├── Axios (withCredentials: true)
        └── httpOnly Cookie (refresh token, auto-sent by browser)
              │
              ▼
  Spring Boot API (port 8080)
        ├── JWT Filter (stateless)
        ├── OAuth2 Client (Google)
        ├── PostgreSQL (users, refresh_tokens)
        └── Redis (OAuth one-time codes, 30s TTL)
```

---

## Backend

### Runtime & Build
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Build**: Maven
- **Port**: 8080
- **Profiles**: `dev` (H2 in-memory), `prod` (PostgreSQL)

### Package Structure (`com.akash.loginsystem`)

```
config/
  AppProperties.java          — Externalized config (JWT, URLs)
  AuthBeanConfig.java         — PasswordEncoder, AuthenticationManager beans
  CorsConfig.java             — CORS (allowCredentials=true, configured origins)
  SecurityConfig.java         — HTTP security, stateless session, OAuth2 login
  WebConfig.java              — Web MVC config

controller/
  AuthController.java         — /api/v1/auth/* endpoints
  UserController.java         — /api/v1/users/* endpoints

dto/
  request/
    LoginRequest.java         — email, password
    RegisterRequest.java      — email, name, password, confirmPassword
    OAuthCodeRequest.java     — code (one-time OAuth exchange code)
    RefreshRequest.java       — refreshToken (internal, pre-Phase 2 compat)
    SetPasswordRequest.java   — password, confirmPassword
    UpdateProfileRequest.java — phone, address fields
  response/
    AuthResponse.java         — accessToken, user (UserSummaryResponse), requiresPasswordSet
    UserResponse.java         — full profile (phone, address, OAuth fields)
    UserSummaryResponse.java  — record: id, email, name, provider, passwordSet, pictureUrl

entity/
  User.java                   — users table (see schema below)
  RefreshToken.java           — refresh_tokens table (see schema below)

exception/
  GlobalExceptionHandler.java
  InvalidCredentialsException.java
  OAuthCodeExpiredException.java
  PasswordAlreadySetException.java
  PasswordMismatchException.java
  PasswordNotSetException.java
  UserAlreadyExistsException.java

model/
  AuthProvider.java           — enum: LOCAL, GOOGLE
  Role.java                   — enum: USER

repository/
  UserRepository.java         — findByEmail, existsByEmail
  RefreshTokenRepository.java — findByToken, findByUserId, deleteByUserId, deleteExpiredTokens

security/
  JwtAuthFilter.java          — Extracts + validates JWT from Authorization header
  JwtProvider.java            — HS256 JWT: generate, validate, extractUserId
  CustomUserDetailsService.java — Loads UserDetails by userId
  oauth2/
    OAuth2UserService.java    — Loads Google profile, upserts User entity
    OAuth2SuccessHandler.java — Post-login: store AuthResponse in Redis, redirect with code
    OAuth2UserPrincipal.java  — OAuth2User wrapper around User entity
    OAuthTokenStore.java      — Redis: store(AuthResponse)→code, consume(code)→AuthResponse

service/
  AuthService.java (interface)
  UserService.java (interface)
  impl/
    AuthServiceImpl.java      — register, login, setPassword, refresh, exchangeOAuthCode, logout
    UserServiceImpl.java      — getMe, updateMe
```

### REST API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | /api/v1/auth/register | None | Register LOCAL user |
| POST | /api/v1/auth/login | None | Email + password login |
| POST | /api/v1/auth/refresh | httpOnly cookie | Rotate access + refresh tokens |
| POST | /api/v1/auth/oauth2/token | None | Exchange one-time OAuth code for tokens |
| POST | /api/v1/auth/set-password | Bearer JWT | Set password for OAuth-only user |
| POST | /api/v1/auth/logout | Bearer JWT | Revoke all refresh tokens |
| GET | /api/v1/users/me | Bearer JWT | Get user profile |
| PUT | /api/v1/users/me | Bearer JWT | Update user profile |

**Public (no auth):** /api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/oauth2/token, /login/oauth2/**, /oauth2/**, /actuator/health, /actuator/info, /h2-console/** (dev)

### Database Schema

**users**
```
id              UUID PK
email           VARCHAR UNIQUE
password        VARCHAR NULLABLE     -- null for OAuth-only users
name            VARCHAR
provider        VARCHAR              -- LOCAL | GOOGLE
provider_id     VARCHAR NULLABLE     -- Google subject ID
picture_url     VARCHAR NULLABLE     -- Google profile picture
password_set    BOOLEAN
role            VARCHAR              -- USER
phone_country_code, phone_number     VARCHAR NULLABLE
address_line1, city, state, zip_code, country  VARCHAR NULLABLE
created_at, updated_at  TIMESTAMP
```

**refresh_tokens**
```
id          UUID PK
token       VARCHAR UNIQUE           -- opaque random UUID
user_id     UUID FK → users.id
expires_at  TIMESTAMP
created_at  TIMESTAMP
```

### Security Model

**Access Token (JWT)**
- Algorithm: HS256
- TTL: 1 hour
- Claims: `sub` (userId), `role`, `iss` (app base URL), `jti`, `iat`, `exp`
- Storage: React state (memory only — lost on page reload)
- Transport: `Authorization: Bearer <token>` header

**Refresh Token (Opaque)**
- Type: Random UUID (not JWT)
- TTL: 7 days
- Storage: PostgreSQL (`refresh_tokens` table)
- Transport: httpOnly cookie
- Cookie attrs: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; MaxAge=604800`
- Rotation: Every `/auth/refresh` call invalidates old token, issues new one

**Password Hashing:** BCrypt (Spring Security default)

**CORS**
- Allowed Origins: `${FRONTEND_URL}` (e.g., http://localhost:3000)
- Allowed Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Allow Credentials: true
- Exposed Headers: Authorization

### Google OAuth2 Flow

```
1. Browser → GET /oauth2/authorization/google
2. Google login
3. OAuth2SuccessHandler: upsert User, build AuthResponse, store in Redis (30s TTL)
4. Redirect browser to frontend /oauth/callback?code=<one-time-code>
5. Frontend → POST /api/v1/auth/oauth2/token { code }
6. Backend: Redis.consume(code) → return accessToken + set refreshToken cookie
```

**Redis key format:** `oauth:code:{uuid}` — atomic getAndDelete(), supports horizontal scaling

### Configuration Profiles

| Profile | Database | DDL Auto | SQL Logging | Redis |
|---------|----------|----------|-------------|-------|
| dev | H2 in-memory | create-drop | true | localhost:6379 |
| prod | PostgreSQL | validate | false | redis:6379 (Docker) |

### Observability
- Spring Actuator: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`
- Micrometer → Prometheus → (Grafana planned)

---

## Frontend

### Runtime & Build
- **Framework**: React 18.3.1
- **Build**: CRA + Craco (`@craco/craco@7.1.0`)
- **Port**: 3000
- **State**: React Context (no Redux)
- **HTTP**: Axios with `withCredentials: true`

### Directory Structure (`frontend/auth-service-mfe/src/`)

```
App.jsx                           — Router + Provider setup, AppNavbar placement
index.js                          — Direct ReactDOM.render (no async bootstrap)

pages/
  HomePage.jsx                    — / (protected, under maintenance)
  login/LoginPage.jsx             — /login
  register/RegisterPage.jsx       — /register
  oauth-callback/OAuthCallbackPage.jsx  — /oauth/callback
  set-password/SetPasswordPage.jsx      — /set-password
  set-password-prompt/SetPasswordPromptPage.jsx — /set-password-prompt
  account-settings/AccountSettingsPage.jsx      — /account-settings

components/
  auth-layout/AuthLayout.jsx      — Wrapper for login/register pages
  button/Button.jsx               — Reusable button (variants: default, google; loading state)
  error-banner/ErrorBanner.jsx    — Error message display
  error-boundary/ErrorBoundary.jsx — React error boundary
  input/Input.jsx                 — Reusable input (label, placeholder, error)
  navbar/Navbar.jsx               — Top nav (hidden on auth paths)
  theme-toggle/ThemeToggle.jsx    — Light/dark toggle

context/
  AuthContext.jsx                 — Global auth state + BroadcastChannel multi-tab sync
  ThemeContext.jsx                — Theme (light/dark)
  createAuthContext.js            — Auth context factory

hooks/
  useAuth.js                      — Access AuthContext (token, user, login, logout, isAuthenticated, initialized)

routes/
  ProtectedRoute.jsx              — Redirects to /login if not authenticated

services/
  api-client/apiClient.js         — Axios instance (baseURL, withCredentials: true, 401 interceptor)
  auth-service/authService.js     — register, login, setPassword, refresh, exchangeOAuthCode, logout
  user-service/userService.js     — getMe, updateMe
```

### Routes

| Path | Component | Protected |
|------|-----------|-----------|
| / | Redirect → /login | No |
| /login | LoginPage | No |
| /register | RegisterPage | No |
| /oauth/callback | OAuthCallbackPage | No |
| /set-password | SetPasswordPage | Yes (temp JWT) |
| /set-password-prompt | SetPasswordPromptPage | No |
| /homepage | HomePage | Yes |
| /account-settings | AccountSettingsPage | Yes |
| * | Redirect → /login | No |

### Auth State & Token Management

**On app load (AuthContext mount):**
1. Call `authService.refresh()` — browser auto-sends httpOnly cookie
2. Backend returns new accessToken
3. Set `accessToken` in React state → `initialized = true`
4. Broadcast `AUTH_INITIALIZED` via `BroadcastChannel('auth')`

**Multi-tab sync (BroadcastChannel):**
- Events: `AUTH_INITIALIZED`, `AUTH_LOGIN`, `AUTH_LOGOUT`
- Receiving tab syncs in-memory accessToken from broadcast
- Prevents race condition during simultaneous token rotation across tabs

**Token storage:**

| Token | Storage | Lifetime | Transmission |
|-------|---------|----------|-------------|
| accessToken | React state (memory) | 1h (lost on reload) | Authorization: Bearer header |
| refreshToken | httpOnly cookie (browser) | 7d | Auto-sent to /api/v1/auth/refresh |

### Navbar Visibility
- Hidden on: /login, /register, /oauth/callback, /set-password, /set-password-prompt
- Visible on: /homepage, /account-settings, and any other authenticated paths
- Google users: circular avatar from `pictureUrl`
- Local users: hamburger menu with dropdown

### Environment Variables
```
REACT_APP_API_URL=http://localhost:8080/api/v1
```

---

## Infrastructure

### Docker Compose Services

| Service | Image | Port | Depends On |
|---------|-------|------|------------|
| postgres | postgres:16-alpine | 5432 | — |
| redis | redis:7-alpine | 6379 | — |
| backend | ./backend/auth-service/Dockerfile | 8080 | postgres, redis (healthy) |
| frontend | ./frontend/auth-service-mfe/Dockerfile | 3000 | backend (healthy) |

**Networks:**
- `internal`: postgres ↔ backend (isolated from host)
- `external`: backend ↔ frontend ↔ host

**Volumes:** `postgres_data` (named, persisted)

### Key Environment Variables (docker-compose)

```
SPRING_PROFILES_ACTIVE
POSTGRES_DB, DB_USERNAME, DB_PASSWORD, DB_URL
JWT_SECRET, JWT_EXPIRY_MS, JWT_REFRESH_EXPIRY_MS
GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
FRONTEND_URL, APP_BASE_URL
REACT_APP_API_URL   (frontend build arg)
```

### Running Locally

```bash
# Development (H2 in-memory, no Docker needed)
cd backend/auth-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend/auth-service-mfe && npm install && npm start

# Production-like (all services via Docker)
docker compose up --build
```

---

## Agentic Setup (`.claude/`)

### Agents
| Agent | Purpose |
|-------|---------|
| backend-architect | Designs backend structure (no implementation) |
| backend-dev | Implements backend per architect specs |
| security-specialist | Security architecture and implementation |
| frontend-dev | Frontend implementation |
| reviewer | Code review and quality checks |
| devops-observability | DevOps, monitoring, Docker setup |
| document-maker | Documentation generation |

### Skills
- `jwt-security.md` — JWT patterns
- `oauth2-google.md` — OAuth2 integration patterns
- `springboot-setup.md` — Spring Boot setup
- `react-ui-guidelines.md` — React UI patterns
- `api-versioning.md` — API versioning strategy
- `rate-limiting.md` — Rate limiting patterns
- `prometheus-grafana.md` — Observability stack

### Memory Files
- `architecture.md` — This file (current live architecture)
- `project_skeleton.md` — Original scaffold
- `conventions.md` — Naming and coding conventions
- `decisions.md` — Architectural decision records
- `MEMORY.md` — Session memory (loaded into system prompt)
- `MEMORY_PREVIOUS.md` — Previous session memory

---

## What Is Not Yet Implemented (Planned)

- Rate limiting on auth endpoints
- Email verification flow
- Password reset / forgot password
- Grafana dashboards (Prometheus metrics exist, dashboards not configured)
- Full test coverage (unit + integration tests)
- CI/CD pipeline
- Role-based access control beyond single USER role
- Account deletion
