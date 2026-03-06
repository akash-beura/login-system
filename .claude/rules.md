# Rules

## Output Rules
1. Never regenerate full files — only generate delta changes.
2. Avoid repeating previous outputs in the same session.
3. Use bullet points for lists, not prose.
4. Keep code concise — no padding, no redundant comments.
5. Avoid rewriting unchanged files.
6. Prefer interfaces first when defining contracts.

## Explanation Rules
7. Never explain basic Spring Boot, React, or Java concepts.
8. Avoid long theory — link to `.claude/memory/architecture.md` instead.

## Self-Improvement Rules
9. All dev agents MUST read `.claude/memory/dev-lessons.md` before starting any task.
10. After any dev task completes, run `dev-task-reviewer` agent to review and extract lessons.
11. New anti-patterns discovered go into `.claude/memory/dev-lessons.md` immediately.
12. Never repeat an error already documented in `dev-lessons.md`.

## Model Routing
Model selection is controlled by agent frontmatter (`model:` field), not by these rules.
- `claude-opus-4-6` — reviewer agent only (deep analysis)
- `claude-sonnet-4-6` — all other agents (implementation, architecture, security, docs, test, task review)
