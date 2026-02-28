# Login System Project

Project setup for a Spring Boot backend and React frontend with Google OAuth2.

## 🤖 Agentic Setup
The `.claude` directory contains the core configuration:
- **Agents**: Roles and model routing for specific tasks (Architecture, Dev, Security, Review).
- **Skills**: Reusable implementation strategies for OAuth2, JWT, and React.
- **Memory**: Living documentation of architecture, conventions, and decisions.
- **Rules**: Token optimization and coding standards defined in `rules.md`.

## 🛠 Project Structure
> [!NOTE]
> The project directories are currently empty placeholde`rs awaiting initialization.

- `login-system-backend`: Spring Boot 3.x (to be initialized)
- `login-system-frontend`: React (to be initialized)

## 🚀 Key Workflows
- **Initialization**: Use `backend-architect` to define the structure, followed by `security-specialist` for the core setup.
- **Backend Build**: `mvn clean install` (in `login-system-backend`)
- **Frontend Start**: `npm start` (in `login-system-frontend`)
- **Quality Check**: Run `reviewer` agent after implementation.

## 📋 Coding Standards
Defined in `.claude/rules.md` and `.claude/memory/conventions.md`.


## 📋 Memory
Till last action memory should be stored in `.claude/memory/MEMORY_PREVIOUS.md` file.