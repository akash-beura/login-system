# Conventions

## Backend Naming
- Controllers: `*Controller`
- Services: `*Service` (interface) + `*ServiceImpl` (implementation)
- Repositories: `*Repository` (extends `JpaRepository`)
- DTOs: `*Request` (inbound) / `*Response` (outbound)
- Entities: singular noun (`User`)
- Exceptions: descriptive noun (`UserNotFoundException`, `TokenExpiredException`)

## API Design
- Prefix: `/api/v1/`
- RESTful resource naming (plural nouns where applicable)
- JSON only (`Content-Type: application/json`)
- Error response shape: `{ "status": 4xx, "error": "Short reason", "message": "Details" }`
- No entity objects in API responses — always use DTOs

## Security
- BCrypt for password hashing (never MD5/SHA)
- JWT access token: 1h TTL, HS256, stored in sessionStorage
- Session token: opaque UUID in Redis, 30min sliding TTL, sent via X-Session-Token header
- No sensitive data (passwords, tokens, UUIDs) in logs
- Email always lowercased (`Locale.ROOT`) at service boundary before DB read/write

## Logging
- Include `traceId` in all log lines (injected via filter)
- Log auth failures at WARN level with masked email (`u***@domain.com`)
- Log auth successes at INFO level
- Never log full request bodies on auth endpoints

## Testing
- Unit test class: `<ClassName>Test` in matching package under `src/test/`
- Test naming: `methodName_givenCondition_expectedBehavior()`
- Prefer `@WebMvcTest` and `@DataJpaTest` over `@SpringBootTest` for speed
- Minimum: test happy path + primary error path for every endpoint

## Git
- Branch naming: `feat/<short-description>`, `fix/<issue>`, `chore/<task>`
- Commit messages: imperative mood, max 72 chars subject line
- Never commit secrets, `.env` files, or JWT tokens

## Frontend
- CSS Modules only — no inline styles, no global CSS overrides
- All colors via CSS custom properties — no hardcoded hex in JSX/module.css
- `REACT_APP_*` environment variables for all configuration
- No `console.log` in production code
