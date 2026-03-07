# Architecture Decisions

1. Stateless JWT over session-based auth.
2. OAuth users stored in same User table.
3. Password nullable for OAuth users initially.
4. First email-login after OAuth forces password set.
5. API versioning from start.
6. Observability built-in from phase 1.
7. Rate limiting to be added in Phase 3.
8. **Session management via Redis**: Each login creates a new Redis session token (30min sliding TTL). Old sessions are not explicitly revoked — they expire via TTL. Future: could add `user-sessions:{userId}` reverse index for immediate revocation of all sessions or single-session enforcement.
9. **Email normalization**: All emails are lowercased (`Locale.ROOT`) at the service boundary (register, login, OAuth) before any DB read/write. The `users` table stores lowercase emails only — this ensures consistent account matching across providers (e.g. LOCAL registered as `User@Example.com`, then Google returns `user@example.com` — both resolve to the same account).
10. **setPassword is account-linking only**: The `/set-password` endpoint is exclusively for OAuth users completing the account-linking flow (`passwordSet=false`). It is blocked with 409 Conflict if the account already has a password set. A future `/change-password` endpoint (requiring old-password verification) is needed for password updates.
11. **sessionStorage over httpOnly cookies**: Switched from httpOnly cookie refresh tokens to sessionStorage JWT + Redis session tokens. Rationale: httpOnly cookies caused page-refresh logout bugs, over-engineered for single-SPA stage. sessionStorage survives refresh, cleared on tab close. Redis session token provides server-side revocation with 30min sliding TTL. No BroadcastChannel needed.