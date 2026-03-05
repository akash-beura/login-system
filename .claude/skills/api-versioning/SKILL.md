# Skill: API Versioning

## Strategy: URL-based versioning
All endpoints prefixed with `/api/v1/`.

## Current Endpoints (v1)
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | None |
| POST | `/api/v1/auth/login` | None |
| POST | `/api/v1/auth/refresh` | httpOnly cookie |
| POST | `/api/v1/auth/oauth2/token` | None |
| POST | `/api/v1/auth/set-password` | Bearer JWT |
| POST | `/api/v1/auth/logout` | Bearer JWT |
| GET | `/api/v1/users/me` | Bearer JWT |
| PUT | `/api/v1/users/me` | Bearer JWT |

## Rules
- Never modify an existing v1 endpoint contract (request/response shape)
- Add new endpoints to v1 if non-breaking
- Create `/api/v2/` only for breaking changes
- All responses: `Content-Type: application/json`
- Error shape: `{ "status": 4xx, "error": "...", "message": "..." }`
