# Skill: Google OAuth2

## Current Implementation (LIVE)
See `.claude/memory/architecture.md` for definitive reference.

## Flow (6 steps)
1. Browser: `GET /oauth2/authorization/google` → redirect to Google consent screen
2. Google callback: `OAuth2SuccessHandler` fires
   - Upsert user in DB (create if new with `provider=GOOGLE, password=null, passwordSet=false`)
   - Build `AuthResponse` (accessToken + requiresPasswordSet)
   - Store in Redis with 30s TTL, key = one-time UUID code
   - Redirect to frontend: `/oauth/callback?code=<uuid>`
3. Frontend `OAuthCallbackPage`: extracts `code` from URL
4. Frontend: `POST /api/v1/auth/oauth2/token` with `{ "code": "<uuid>" }`
5. Backend: consumes Redis code (deletes atomically), returns `AuthResponse` + sets httpOnly refresh cookie
6. Frontend: stores accessToken in React state, navigates to `/` or `/set-password-prompt`

## Key Design Decisions
- Redis one-time code prevents token exposure in browser URL/history
- Redis TTL = 30s — code expires quickly to limit replay window
- New Google users get `requiresPasswordSet=true` in response — frontend prompts for password
- `setPassword` endpoint exclusively for OAuth users with `passwordSet=false`

## Key Files
- `security/OAuth2SuccessHandler.java` — post-auth handler
- `security/OAuth2UserService.java` — user upsert logic
- `security/OAuthTokenStore.java` — Redis operations
- `controller/AuthController.java` → `exchangeOAuthCode()` method
- `frontend/auth-service-mfe/src/pages/oauth-callback/` — OAuthCallbackPage
