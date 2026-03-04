# Platform Restructure — From "Login System" to Reusable Microservice Platform

## Problem

The current repo is a single project (`login-system`) that mixes platform-level concerns (auth, OAuth, JWT, user management) with product-level concerns (resume-builder). Adding a second product (e.g. job-fetcher) would mean either cramming it into the same backend or duplicating infrastructure setup.

## Proposed Directory Structure

```
akash-platform/                        ← renamed from login-system
│
├── docker-compose.yml                 ← orchestrates EVERYTHING
├── docker-compose.override.yml        ← local dev overrides (optional)
├── .env.example
├── .env
│
├── infrastructure/                    ← shared infra configs
│   ├── nginx/                         ← API gateway / reverse proxy
│   │   └── nginx.conf                 ← routes /api/auth → auth-service:8080
│   │                                     routes /api/resume → resume-service:8081
│   └── init-scripts/
│       └── init-databases.sql         ← creates per-service databases on postgres startup
│
├── platform/                          ← ★ SHARED PLATFORM LAYER
│   └── auth-service/                  ← current login-system-backend, renamed
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
│
├── services/                          ← ★ PRODUCT MICROSERVICES (each is independent)
│   ├── resume-builder/                ← new Spring Boot service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   │
│   └── job-fetcher/                   ← future service
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
│
└── frontend/                          ← ★ ALL FRONTENDS
    ├── landing-page/                  ← shared: login, register, OAuth, homepage
    │   ├── Dockerfile
    │   ├── package.json
    │   └── src/
    │
    ├── resume-builder/                ← product frontend
    │   ├── Dockerfile
    │   ├── package.json
    │   └── src/
    │
    └── job-fetcher/                   ← future product frontend
        └── ...
```

## Key Architectural Decisions

### Backend: Why `platform/` vs `services/`?

| Layer | Purpose | Example |
|-------|---------|---------|
| `platform/` | Shared foundational services that ALL products depend on | `auth-service` (JWT, OAuth, user management) |
| `services/` | Product-specific microservices with their own DB schema | `resume-builder`, `job-fetcher` |

The `auth-service` stays exactly as it is today — no code changes needed. New services just validate JWTs it issues.

### How new backend services authenticate requests

Each new service (e.g. `resume-builder`) doesn't re-implement auth. It just:
1. Adds `spring-boot-starter-oauth2-resource-server` to its `pom.xml`
2. Configures `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` pointing at the auth-service
3. All incoming requests are authenticated by verifying the JWT signature — zero coupling to user DB

### Frontend: How products plug in

Each product can be:
- **Option A — Separate React app** (recommended for now): Each frontend is a standalone CRA/Vite app that calls the auth-service for login and its own backend for data. The shared landing-page handles auth UI.
- **Option B — Module Federation** (later, if needed): Shell app lazy-loads product remotes. More complex, only worth it when you have shared chrome/nav.

### API Gateway (Nginx reverse proxy)

A single entry point routes traffic to the right service:

```
http://localhost/                → landing-page (frontend, port 3000)
http://localhost/resume-builder  → resume-builder frontend (port 3001)
http://localhost/api/auth/*      → auth-service (port 8080)
http://localhost/api/resume/*    → resume-builder-service (port 8081)
http://localhost/api/jobs/*      → job-fetcher-service (port 8082)
```

### Database isolation

Each service gets its own database on the same Postgres instance (cheaper than running multiple Postgres containers):

```sql
-- infrastructure/init-scripts/init-databases.sql
CREATE DATABASE auth_db;
CREATE DATABASE resume_db;
CREATE DATABASE jobs_db;
```

## User Review Required

> [!IMPORTANT]
> **Rename decision**: I suggest renaming the repo from `login-system` to something like `akash-platform` or `unified-platform`. The current name is product-specific, and you're building a multi-product platform. Do you have a preferred name?

> [!IMPORTANT]
> **Frontend strategy**: For the resume-builder you already have, do you want to keep it as a standalone React app in `frontend/resume-builder/` (simpler), or re-introduce Module Federation (the old shell/remote pattern)? I recommend standalone apps for now.

> [!IMPORTANT]
> **Scope check**: Should I only restructure (move directories, update docker-compose, create the nginx gateway config) in this task? Or also scaffold the `resume-builder` backend service?

## Proposed Changes

### 1. Directory moves (no code changes)

| Current Path | New Path |
|---|---|
| `login-system-backend/` | `platform/auth-service/` |
| `frontend/resume-builder-front-end/` | `frontend/resume-builder/` |

### 2. New files

| File | Purpose |
|---|---|
| `infrastructure/nginx/nginx.conf` | API gateway routing config |
| `infrastructure/init-scripts/init-databases.sql` | Per-service DB creation |

### 3. Updated files

| File | Changes |
|---|---|
| `docker-compose.yml` | New paths, add nginx service, add resume-builder service block |
| `.env.example` | Add new service ports/URLs |
| `.gitignore` | Update paths to match new structure |

## Verification Plan

### Manual Verification
1. `docker compose up --build` — all services start without errors
2. Navigate to `http://localhost` — landing page loads
3. Login via Google OAuth — JWT issued and works
4. Navigate to `http://localhost/resume-builder` — resume-builder frontend loads
5. API calls from resume-builder frontend hit `http://localhost/api/resume/*` and pass through auth
