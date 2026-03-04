# Login System Project

Project setup for a Spring Boot backend and React frontend with Google OAuth2.

## 🤖 Agentic Setup
The `.claude` directory contains the core configuration:
- **Agents**: Roles and model routing for specific tasks (Architecture, Dev, Security, Review).
- **Skills**: Reusable implementation strategies for OAuth2, JWT, and React.
- **Memory**: Living documentation of architecture, conventions, and decisions.
- **Rules**: Token optimization and coding standards defined in `rules.md`.

## 🛠 Project Structure
- `login-system-backend`: Spring Boot 3.x
- `login-system-frontend`: React

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
- Available agents: Architecture, Dev, Security, Review (see `.claude` directory)

### 3. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness
- Run `reviewer` agent after implementation for quality checks

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes — don't over-engineer
- Challenge your own work before presenting it

### 6. Autonomous Bug Fixing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests — then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

## 🚀 Key Build Commands
- **Backend Build**: `mvn clean install` (in `login-system-backend`)
- **Frontend Start**: `npm start` (in `login-system-frontend`)

## Task Management
1. **Plan First**: Write plan to `tasks/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `tasks/todo.md`
6. **Capture Lessons**: Update `tasks/lessons.md` after corrections

## 📋 Coding Standards
Defined in `.claude/rules.md` and `.claude/memory/conventions.md`.

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

## 📋 Memory
Till last action memory should be stored in `.claude/memory/MEMORY_PREVIOUS.md` file.