# Skill: JWT Security

## Current Implementation (LIVE — Phase 2)
See `.claude/memory/architecture.md` for definitive reference.

## Token Architecture
- **Access token**: HS256 JWT, 1h TTL, returned in JSON response body
  - Claims: `sub` (userId UUID), `iat`, `exp`, `role`, `iss`, `jti`
  - Stored in React state (memory) — never in localStorage
  - Sent as `Authorization: Bearer <token>` header
- **Refresh token**: Opaque UUID, 7d TTL, stored in PostgreSQL `refresh_tokens` table
  - Returned as httpOnly cookie — JavaScript cannot access it
  - Cookie attrs: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; MaxAge=604800`
  - Browser sends it automatically on requests to `/api/v1/auth/refresh`

## Token Rotation
- `POST /api/v1/auth/refresh` — consumes old refresh token, issues new access token + new refresh token cookie
- Single active session per user: `issueTokens()` calls `deleteByUser()` before creating new refresh token
- Future multi-device: add `deviceId` or `tokenFamily` to `RefreshToken` entity

## Multi-Tab Sync
- AuthContext creates `BroadcastChannel('auth')` on mount
- Login/logout/refresh broadcast new access token to all tabs
- Prevents race condition when multiple tabs trigger refresh simultaneously

## Filter Chain
- `JwtAuthFilter` extends `OncePerRequestFilter`
- Extracts Bearer token from `Authorization` header
- Validates signature, expiry, issuer
- Sets `SecurityContext` with `UsernamePasswordAuthenticationToken`

## Key Files
- `security/JwtProvider.java` — generation + validation
- `security/JwtAuthFilter.java` — filter
- `config/SecurityConfig.java` — filter registration
- `frontend/auth-service-mfe/src/services/api-client/apiClient.js` — `withCredentials: true`
- `frontend/auth-service-mfe/src/context/AuthContext.jsx` — in-memory token + BroadcastChannel
