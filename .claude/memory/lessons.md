# Lessons Learned (Orchestrator Level)

Captures patterns from mistakes, corrections, and debugging sessions at the orchestrator/workflow level.
Dev-specific anti-patterns go in `dev-lessons.md` instead.

## Format
```
### [Date] - [Short description]
**What happened**: ...
**Root cause**: ...
**Rule going forward**: ...
```

---

### 2026-03-05 — Never trust memory files over filesystem verification
**What happened**: MEMORY.md had stale paths (`login-system-backend/`, `frontend/landing-page/`). I "fixed" CLAUDE.md to match the stale memory instead of verifying against the actual filesystem. The real paths are `backend/auth-service/` and `frontend/auth-service-mfe/`.
**Root cause**: Used memory as source of truth without running `ls` to verify.
**Rule going forward**: Before changing any path reference, ALWAYS `ls` the actual directory first. Memory files can be stale — the filesystem doesn't lie.
