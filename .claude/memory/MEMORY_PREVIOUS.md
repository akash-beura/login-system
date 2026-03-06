# Previous Session Delta

This file records only what changed between the previous session and the current one.
Full system state is in `.claude/memory/MEMORY.md` and `.claude/memory/architecture.md`.

## Last Major Change
- Phase 2: Moved refresh token from JSON body to httpOnly cookie
- Added BroadcastChannel multi-tab sync to AuthContext
- Added CORS `allowCredentials: true`
- Added `pictureUrl` to User entity and `UserSummaryResponse`
- Google users show circular avatar; local users show hamburger dropdown

## Previous State (before Phase 2)
- Refresh token was returned in JSON response body
- Frontend stored refresh token in localStorage
- No multi-tab sync
