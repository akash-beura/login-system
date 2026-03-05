# Skill: React UI Guidelines

## Current Implementation (LIVE)
See `.claude/memory/architecture.md` for definitive reference.

## Token Storage (CRITICAL — do not deviate)
- **Access token**: React state (memory) — lost on page reload, refreshed via cookie
- **Refresh token**: httpOnly cookie — browser manages, JavaScript cannot read it
- **Never**: localStorage, sessionStorage, or any persistent browser storage for tokens

## Design System
- CSS Modules for all component styles (`.module.css`)
- CSS custom properties for all colors and spacing:
  - `--bg-app`, `--bg-card`, `--text-primary`, `--text-secondary`
  - `--btn-primary`, `--focus-ring`
  - Light theme: `--bg-app: #f0f2f5`, `--btn-primary: #1877f2`
  - Dark theme: `--bg-app: #18191a`, `--btn-primary: #2d88ff`
- ThemeContext defaults to `dark`, persists to localStorage
- ThemeToggle fixed bottom-right

## Structure
```
src/
  pages/          # One directory per page
  components/
    common/       # Shared UI (Button, Input, ErrorBanner, etc.)
  context/        # AuthContext, ThemeContext
  services/       # apiClient.js, authService.js, userService.js
```

## API Client Rules
- `apiClient` (Axios): `withCredentials: true` always — required for cookie passthrough
- 401 interceptor: attempt token refresh via `POST /api/v1/auth/refresh`, retry once, then logout
- Base URL from `REACT_APP_API_URL` environment variable

## Error Handling
- Display user-friendly messages — never expose raw error objects or stack traces
- 401 triggers global logout flow
- Network errors show generic "Something went wrong" message

## Routing
- `ProtectedRoute` wrapper for auth-required pages
- Auth paths (`/login`, `/register`, `/oauth/callback`, etc.) hide Navbar
- Google users show circular avatar from `user.pictureUrl`
- Local users show hamburger menu with dropdown
