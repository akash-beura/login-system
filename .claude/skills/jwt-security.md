# Skill: JWT Security

## Requirements
- HS256 or RS256
- Expiration required
- Refresh token optional (Phase 2)

## Flow
1. User authenticates
2. Generate JWT
3. Return in response body
4. Validate in filter
5. Set SecurityContext

## Claims
- userId
- email
- roles