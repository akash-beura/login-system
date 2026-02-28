# Architecture Decisions

1. Stateless JWT over session-based auth.
2. OAuth users stored in same User table.
3. Password nullable for OAuth users initially.
4. First email-login after OAuth forces password set.
5. API versioning from start.
6. Observability built-in from phase 1.
7. Rate limiting to be added in Phase 3.
8. **Single active session per user (intentional)**: `issueTokens()` calls `deleteByUser()` before creating a new refresh token, meaning login on a new device invalidates the previous device's session. This is a deliberate security trade-off — simpler token management at the cost of multi-device support. To support multi-device in the future, add a `deviceId` or `tokenFamily` column to `RefreshToken` and scope `deleteByUser` to that family.
9. **Email normalization**: All emails are lowercased (`Locale.ROOT`) at the service boundary (register, login, OAuth) before any DB read/write. The `users` table stores lowercase emails only — this ensures consistent account matching across providers (e.g. LOCAL registered as `User@Example.com`, then Google returns `user@example.com` — both resolve to the same account).
10. **setPassword is account-linking only**: The `/set-password` endpoint is exclusively for OAuth users completing the account-linking flow (`passwordSet=false`). It is blocked with 409 Conflict if the account already has a password set. A future `/change-password` endpoint (requiring old-password verification) is needed for password updates.