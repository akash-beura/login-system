# Product & Infrastructure Roadmap

## Product Vision
A resume management and job-finding platform built on the existing auth system.
Mono-repo, Spring Boot microservices, GCP-native storage, Kubernetes-first deployment.

---

## Architecture Overview

### Microservices

| Service | Responsibility | DB |
|---|---|---|
| `auth-service` | JWT issuance, OAuth2, user identity | postgres schema: `auth` |
| `api-gateway` | JWT validation, routing, rate limiting, CORS | stateless |
| `resume-manager-service` | upload, versioning, GCP Storage, metadata CRUD | postgres schema: `resume_manager` |
| `resume-reader-service` | parse PDF/docx → structured data (text, skills, keywords) | postgres schema: `resume_reader` |
| `ai-recommendation-service` | cover letter gen, keyword extraction, job scoring (Claude API) | postgres schema: `ai_recommendation` |
| `job-service` (Phase 2) | LinkedIn integration, saved jobs | postgres schema: `jobs` |

### Key Architecture Decisions
- **API Gateway** (Spring Cloud Gateway): validates JWT, injects `X-User-Id` + `X-User-Email` headers, all downstream services trust these headers — zero coupling to auth-service
- **Database**: single Postgres instance, separate schema per service (owned by that service only — no cross-schema queries)
- **Inter-service comms**: REST for now; Kafka added later for async events (`resume.uploaded`, `resume.parsed`)
- **AI features live in backend**: API keys server-side, results cacheable in Redis, independently scalable
- **Resume parsing** (PENDING DECISION): in-house (Apache POI + PDFBox) vs third-party API (Affinda, Sovren)
- **LinkedIn API** (PENDING DECISION for Phase 2): official API vs RapidAPI proxy

### Request Flow
```
Browser → API Gateway (JWT validate → inject X-User-Id)
       → resume-manager-service  (upload / list / delete)
       → resume-reader-service   (parse trigger on upload)
       → ai-recommendation-service (cover letter / keyword extract)
       → job-service             (Phase 2: search + save jobs)
```

### Sync REST call chain (Phase 1)
```
POST /api/v1/resumes
  → resume-manager-service saves file to GCP Storage, saves metadata to DB
  → calls resume-reader-service POST /internal/parse (resumeId, gcpPath)
  → resume-reader-service parses file, stores structured data (skills, text)
  → returns parsed summary back to resume-manager-service
  → resume-manager-service returns upload response to client
```

---

## Phase 1 — Resume Management

### Backend Endpoints

**resume-manager-service** (port 8081)
- `POST /api/v1/resumes` — multipart upload (PDF/docx, max 10MB)
- `GET /api/v1/resumes` — list all versions for current user
- `GET /api/v1/resumes/{id}/url` — get signed GCP URL for preview/download
- `PATCH /api/v1/resumes/{id}/activate` — set as active version
- `DELETE /api/v1/resumes/{id}` — soft delete

**resume-reader-service** (port 8082)
- `POST /internal/parse` — parse a resume by GCP path, return structured data
- `GET /internal/resume/{resumeId}/data` — fetch parsed structured data

**ai-recommendation-service** (port 8083)
- `POST /api/v1/cover-letter/generate` — body: `{ resumeId, jobDescription }` → returns cover letter text
- Results cached in Redis (key: hash of resumeId + jobDescription)

### Frontend Pages
- `/home` — resume upload dropzone (PDF/docx), version list, activate/delete, preview modal
- `/cover-letter` — select resume version + paste job description → generate + copy/download

### Infrastructure (Phase 1)
- Docker Compose: postgres, redis, fake-gcs-server, api-gateway, resume-manager-service, resume-reader-service, ai-recommendation-service, frontend
- `application-dev.yaml`: fake-gcs endpoint + local postgres
- `application-prod.yaml`: real GCP Cloud Storage + Cloud SQL

---

## Phase 2 — Job Finder

### Backend Endpoints

**job-service** (port 8084)
- `POST /api/v1/jobs/search` — extracts keywords (calls ai-recommendation-service), queries LinkedIn API, returns ranked list
- `GET /api/v1/jobs/saved` — list saved jobs
- `POST /api/v1/jobs/saved` — save a job
- `DELETE /api/v1/jobs/saved/{id}` — unsave

**ai-recommendation-service additions**
- `POST /internal/keywords/extract` — extract job-search keywords from resume text

### Frontend Pages
- `/find-jobs` — keyword chips (editable, pre-filled from resume), job card grid, save/unsave, apply → LinkedIn

---

## Infrastructure Progression

### Stage 1 — Local Docker Compose
- All services in `docker-compose.yml`
- fake-gcs-server for GCP Storage emulation
- `.env.local` for secrets (never committed)
- Makefile targets: `make up`, `make down`, `make build`

### Stage 2 — Kubernetes (local first, then GKE)
- `k8s/base/` — shared manifests (Deployments, Services, ConfigMaps)
- `k8s/overlays/dev/` and `k8s/overlays/prod/` — Kustomize overlays
- Start local: minikube or kind
- Move to GKE Autopilot for cloud
- Secrets: Kubernetes Secrets locally → GCP Secret Manager + Workload Identity in prod

### Stage 3 — GitHub Actions CI/CD
- **PR Pipeline** (every PR):
  - Build + unit tests (Maven + npm)
  - Docker build (no push)
  - AI Code Review: Claude API reviews diff, posts PR comments
  - Security scan: OWASP dependency check + Trivy image scan
- **Main Pipeline** (merge to main):
  - Build + test + Docker build + push to GCP Artifact Registry
  - Deploy to Dev (automatic)
  - Deploy to Prod (manual approval gate)

### Stage 4 — ArgoCD + Argo Rollouts
- ArgoCD: GitOps — k8s manifests in repo, ArgoCD syncs cluster state to git
- App-of-Apps pattern: separate ArgoCD apps for dev and prod
- Argo Rollouts: canary or blue-green deployments for prod
- Notifications: ArgoCD → Slack on sync/failure events

---

## Mono-Repo Structure (Target)

```
login-system/
  backend/
    auth-service/                 # existing
    api-gateway/                  # Spring Cloud Gateway
    resume-manager-service/
    resume-reader-service/
    ai-recommendation-service/
    job-service/                  # Phase 2
  frontend/
    auth-service-mfe/             # existing + new pages
  k8s/
    base/
    overlays/
      dev/
      prod/
  .github/
    workflows/
      pr.yml
      main.yml
  docker-compose.yml
  Makefile
  GCP_Deployment_Guide.md
```

---

## Key Tech Choices

| Concern | Choice |
|---|---|
| File storage | GCP Cloud Storage |
| Local storage emulation | fake-gcs-server (Docker) |
| Database | PostgreSQL (separate schema per service) |
| Caching | Redis |
| AI features | Claude API (Anthropic SDK) |
| LinkedIn jobs | PENDING — official API vs RapidAPI proxy |
| Resume parsing | PENDING — Apache POI + PDFBox vs third-party |
| API Gateway | Spring Cloud Gateway |
| Container registry | GCP Artifact Registry |
| k8s cluster | GKE Autopilot |
| GitOps | ArgoCD |
| Progressive delivery | Argo Rollouts |
| CI/CD | GitHub Actions |
| Secrets (prod) | GCP Secret Manager + Workload Identity |
| k8s config management | Kustomize |

---

## Open Decisions
1. Resume parsing: in-house (Apache POI + PDFBox) vs third-party API?
2. LinkedIn API: official (requires app approval) vs RapidAPI proxy (faster dev, unofficial)?
3. GCP environment setup: single project vs separate projects per env? (see GCP_Deployment_Guide.md)
