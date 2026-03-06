# Project Memory — Login System

## Current Phase
Phase 2 complete. Security-hardened. Ready for Phase 3 (rate limiting, resume management).

## Quick Reference
- **Full architecture**: `.claude/memory/architecture.md`
- **Conventions**: `.claude/memory/conventions.md`
- **Decisions**: `.claude/memory/decisions.md`
- **Dev lessons**: `.claude/memory/dev-lessons.md`
- **Orchestrator lessons**: `.claude/memory/lessons.md`
- **Roadmap**: `.claude/memory/roadmap.md`

## Key Facts (not duplicated in architecture.md)
- API base: /api/v1
- Group: com.akash.loginsystem
- Password is nullable for OAuth users; passwordSet=false until they set one
- `RegisterRequest` requires `confirmPassword` field

## Docker / DevOps
- Services: postgres, redis, backend, frontend
- All secrets externalized via env vars
- Backend Dockerfile: multi-stage Maven build (self-contained)
- Designed for future: Argo CD → Kubernetes → GCP

## Critical Rules
- Always `ls` actual directories before updating path references
- Use `npm install --legacy-peer-deps` for this project
- Never set `ddl-auto: create-drop` or `create` in prod profile
