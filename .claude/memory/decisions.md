# Architecture Decisions

1. Stateless JWT over session-based auth.
2. OAuth users stored in same User table.
3. Password nullable for OAuth users initially.
4. First email-login after OAuth forces password set.
5. API versioning from start.
6. Observability built-in from phase 1.
7. Rate limiting to be added in Phase 3.