# Skill: Rate Limiting

## Status: Phase 3 — Not Yet Implemented
Rate limiting is planned. Do not add it without explicit instruction.

## Planned Strategy
- Library: **Bucket4j** with Redis backend (Redis already in stack)
- Scope: IP-based throttling per endpoint
- Target endpoints:
  - `POST /api/v1/auth/login` — strict (5 req/min per IP)
  - `POST /api/v1/auth/register` — moderate (10 req/min per IP)
  - `POST /api/v1/auth/refresh` — lenient (60 req/min per IP)

## Integration Point
- Spring Filter or `HandlerInterceptor` before `JwtAuthFilter`
- Return `429 Too Many Requests` with `Retry-After` header
- Do not count successful auths toward rate limit — only failures

## Why Redis (not in-memory)
Redis is already in the stack for OAuth code storage. Using Redis for rate limiting ensures limits hold across multiple backend instances.

## Key Files to Modify (when implementing)
- `config/SecurityConfig.java` — register rate limit filter
- New: `security/RateLimitFilter.java`
- New: `config/RateLimitConfig.java` (Bucket4j + Redis config)
