# Login System Project

Project setup for a Spring Boot backend and React frontend with Google OAuth2.

## 🤖 Agentic Setup
The `.claude` directory contains the core configuration:
- **Agents**: Roles and model routing for specific tasks (Architecture, Dev, Security, Review, Test).
- **Skills**: Reusable implementation strategies for OAuth2, JWT, and React.
- **Memory**: Living documentation of architecture, conventions, and decisions.
- **Rules**: Coding standards defined in `rules.md`.

## 🛠 Project Structure
- `backend/auth-service/`: Spring Boot 3.x (auth, OAuth, JWT) — port 8080
- `frontend/auth-service-mfe/`: React SPA (login, register, account settings) — port 3000

## 📐 Architecture Reference
- **Current architecture**: `.claude/memory/architecture.md` — exact live architecture (packages, endpoints, schema, flows)
- **Original scaffold**: `.claude/memory/project_skeleton.md` — high-level skeleton used to bootstrap the project

## Workflow Orchestration

### 1. Plan Mode Default
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- If something goes sideways, STOP and re-plan immediately — don't keep pushing
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity
- Use `backend-architect` agent to define structure, `security-specialist` for core setup

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean
- Offload research, exploration, and parallel analysis to subagents
- For complex problems, throw more compute at it via subagents
- One task per subagent for focused execution
- Available agents: `backend-architect`, `backend-dev`, `security-specialist`, `frontend-dev`, `reviewer`, `devops-observability`, `document-maker`, `test-writer`

### 3. Self-Improvement Loop
- After ANY correction from the user: update `.claude/memory/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness
- Run `reviewer` agent after implementation for quality checks

### 5. Code Quality Criteria
For non-trivial changes, verify before submitting:
- No method exceeds 30 lines
- No class exceeds 300 lines
- No nested callbacks/conditions deeper than 2 levels
- No duplication across 3+ locations (extract to shared utility)
- Every public API method has validation at the boundary

### 6. Autonomous Bug Fixing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests — then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

## 🚀 Key Build Commands
- **Backend Build**: `mvn clean install` (in `backend/auth-service/`)
- **Frontend Start**: `npm start` (in `frontend/auth-service-mfe/`)
- **Full Stack**: `docker compose up --build` (from repo root)

## Task Management
1. **Plan First**: Use native `TodoWrite` tool to track tasks with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Capture Lessons**: Update `.claude/memory/lessons.md` after corrections

## 📋 Coding Standards
Defined in `.claude/rules.md` and `.claude/memory/conventions.md`.

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

## 📋 Memory
Key session state is captured in `.claude/memory/MEMORY.md`. Lessons go in `.claude/memory/lessons.md`.
