# Conventions

## Naming
- Controllers: *Controller
- Services: *Service
- Repositories: *Repository
- DTOs: *Request / *Response

## API
- Prefix: /api/v1
- RESTful design
- JSON only

## Security
- BCrypt password hashing
- JWT expiry required
- No session state

## Logging
- Include traceId
- Do not log passwords
- Log authentication failures