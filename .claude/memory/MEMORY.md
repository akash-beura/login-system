# Project Memory — Login System

## Current Phase
Architecture defined. Ready for implementation phase.

## Backend
- Group: com.akash.loginsystem
- Root package layers: controller, service/impl, repository, security, config, entity, dto/request, dto/response, exception, model
- Key entities: User (with passwordSet flag), RefreshToken
- AuthProvider enum: LOCAL, GOOGLE
- Password is nullable for OAuth users; passwordSet=false until they set one
- API base: /api/v1

## Account Linking Flow
- POST /api/v1/auth/login → if OAuth user + passwordSet=false → return requiresPasswordSet:true
- Client routes to SetPasswordPage → POST /api/v1/auth/set-password → sets password, flips flag

## Frontend
- Pages: LoginPage, RegisterPage, SetPasswordPage, HomePage (Under Maintenance)
- Design tokens: bg #f0f0f0 (silver), btn #1a3a6b (dark blue), rounded corners
- Auth state via React Context (AuthContext)
- Axios in services/authService.js

## Docker / DevOps
- Services: postgres, backend, frontend
- All secrets externalized via env vars (DB_URL, JWT_SECRET, GOOGLE_CLIENT_ID, etc.)
- Designed for future migration: Argo CD → Kubernetes → GCP

## Decisions Log
- See decisions.md for numbered ADRs
- JWT stateless, no sessions
- Same User table for LOCAL and GOOGLE providers
