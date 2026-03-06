# GCP Deployment Guide — End-to-End Flow

This document walks through how the application is deployed on GCP from code push to
running in production, and explains the single-project vs multi-project decision.

---

## GCP Services Involved

| GCP Service | What it does in this app |
|---|---|
| **GKE Autopilot** | Runs all microservices as Kubernetes pods |
| **Cloud SQL (Postgres)** | Managed Postgres — one instance, separate schemas per service |
| **Cloud Storage** | Stores resume files (PDF/docx) |
| **Artifact Registry** | Stores Docker images built by GitHub Actions |
| **Secret Manager** | Stores DB passwords, API keys, JWT secrets |
| **Workload Identity** | Lets GKE pods access GCP services without a service account key file |
| **Cloud Load Balancer** | Exposes the API Gateway and frontend to the internet (via GKE Ingress) |
| **Cloud Armor** (optional) | WAF / DDoS protection on the Load Balancer |
| **Cloud DNS** | Maps your domain to the Load Balancer IP |
| **Artifact Registry** | Docker image storage (replaces Docker Hub) |

---

## Option A — Single GCP Project

### Structure
```
GCP Project: my-app-project
  ├── GKE Cluster: my-app-cluster
  │     ├── Namespace: dev
  │     │     ├── auth-service (dev)
  │     │     ├── resume-manager-service (dev)
  │     │     └── ... all services
  │     └── Namespace: prod
  │           ├── auth-service (prod)
  │           ├── resume-manager-service (prod)
  │           └── ... all services
  ├── Cloud SQL: one instance, databases: app_dev, app_prod
  ├── Cloud Storage: buckets: my-app-resumes-dev, my-app-resumes-prod
  ├── Artifact Registry: one repo with image tags (dev, prod)
  └── Secret Manager: secrets namespaced by env (e.g. prod/jwt-secret, dev/jwt-secret)
```

### Pros
- Simpler to manage — one billing account, one console view
- Cheaper — shared GKE cluster, shared Cloud SQL instance
- Faster to set up — one set of IAM roles, one Workload Identity config

### Cons
- A misconfigured deployment can accidentally affect prod (e.g. wrong namespace)
- Less isolation — a quota exhaustion in dev can starve prod
- Audit trail is mixed — harder to tell prod vs dev changes in logs

---

## Option B — Separate GCP Projects per Environment

### Structure
```
GCP Project: my-app-dev
  ├── GKE Cluster: my-app-dev-cluster (Namespace: default)
  ├── Cloud SQL: my-app-dev-db
  ├── Cloud Storage: my-app-resumes-dev
  └── Secret Manager: jwt-secret, db-password, ...

GCP Project: my-app-prod
  ├── GKE Cluster: my-app-prod-cluster (Namespace: default)
  ├── Cloud SQL: my-app-prod-db
  ├── Cloud Storage: my-app-resumes-prod
  └── Secret Manager: jwt-secret, db-password, ...
```

### Pros
- Strong blast-radius isolation — a broken dev deploy cannot touch prod
- Separate billing — see exact cost of dev vs prod
- Separate IAM — developers can have editor on dev, read-only on prod
- Cleaner audit logs per environment
- Industry standard for regulated or enterprise-grade apps

### Cons
- More setup — two sets of GKE clusters, IAM, networking
- Higher cost — two GKE clusters (even Autopilot has a base fee ~$74/month each)
- More GitHub Actions config — two sets of credentials

---

## Recommendation

**Start with Option A (single project), migrate to Option B when you have a stable prod.**

Rationale:
- You are in early development — the overhead of two projects slows you down
- Kubernetes namespaces (dev/prod) give you good enough isolation for now
- When you have real users on prod, migrate prod to its own project — GCP makes this
  relatively straightforward with Terraform

---

## End-to-End Deployment Flow (GitHub Actions → GCP)

### Step 1 — Developer pushes code

```
git push origin feature/resume-upload
→ opens PR
→ GitHub Actions PR pipeline triggers
```

### Step 2 — PR Pipeline runs

```
GitHub Actions pr.yml:
  1. Checkout code
  2. Run unit tests (Maven + npm)
  3. Docker build (no push — just verify it builds)
  4. OWASP dependency check
  5. Trivy image vulnerability scan
  6. Claude AI Code Review:
       - Sends git diff to Claude API
       - Claude posts review comments on the PR
  7. All checks pass → PR is ready to merge
```

### Step 3 — Merge to main

```
git merge → main branch
→ GitHub Actions main.yml triggers
```

### Step 4 — Main Pipeline: Build & Push

```
GitHub Actions main.yml:
  1. Checkout code
  2. Run full test suite
  3. Docker build all changed services
  4. Authenticate to GCP:
       - Uses Workload Identity Federation (no long-lived keys)
       - GitHub OIDC token → GCP service account
  5. Push Docker images to GCP Artifact Registry:
       e.g. us-central1-docker.pkg.dev/my-app-project/backend/resume-manager:sha-abc123
  6. Update k8s/overlays/dev/image-tags.yaml with new image digest
  7. Commit + push that change → ArgoCD detects it → auto-deploys to dev
```

### Step 5 — ArgoCD deploys to Dev automatically

```
ArgoCD watches the git repo (k8s/overlays/dev/)
→ detects new image tag commit
→ syncs GKE dev namespace
→ rolling update of affected pods
→ health checks pass → sync complete
→ Slack notification: "Dev deploy complete"
```

### Step 6 — Promote to Prod (manual approval)

```
GitHub Actions main.yml (continued):
  8. Wait for manual approval (GitHub Environments protection rule)
     → a team member approves in GitHub UI
  9. Update k8s/overlays/prod/image-tags.yaml with same image digest
  10. Commit + push → ArgoCD detects it → deploys to prod
```

### Step 7 — ArgoCD deploys to Prod via Argo Rollouts

```
ArgoCD watches k8s/overlays/prod/
→ detects change → triggers Argo Rollout (canary strategy)
→ sends 10% traffic to new version
→ monitors error rate + latency for 5 minutes
→ if healthy: promote to 100%
→ if unhealthy: auto-rollback to previous version
→ Slack notification: "Prod deploy complete" or "Prod rollback triggered"
```

---

## Visual Summary

```
Developer
   │
   ▼
git push → PR opened
   │
   ▼
GitHub Actions (PR pipeline)
   ├── Unit tests
   ├── Docker build check
   ├── Security scan
   └── AI Code Review (Claude API → PR comments)
   │
   ▼
PR merged to main
   │
   ▼
GitHub Actions (main pipeline)
   ├── Full tests
   ├── Docker build + push → GCP Artifact Registry
   └── Update k8s image tag in git (dev overlay)
          │
          ▼
       ArgoCD detects git change
          │
          ▼
       GKE Dev Namespace — auto deploy
          │
          ▼
       Manual approval in GitHub
          │
          ▼
       Update k8s image tag in git (prod overlay)
          │
          ▼
       ArgoCD detects git change
          │
          ▼
       Argo Rollouts — canary deploy to GKE Prod
          ├── 10% traffic → new version
          ├── monitor for 5 min
          └── promote to 100% or rollback
```

---

## GCP Setup Checklist (when you're ready)

### One-time setup
- [ ] Create GCP project(s)
- [ ] Enable APIs: GKE, Cloud SQL, Cloud Storage, Artifact Registry, Secret Manager
- [ ] Create GKE Autopilot cluster
- [ ] Create Cloud SQL Postgres instance + schemas per service
- [ ] Create Cloud Storage buckets (one per env)
- [ ] Create Artifact Registry repo
- [ ] Configure Workload Identity Federation for GitHub Actions
- [ ] Store secrets in Secret Manager
- [ ] Configure ExternalSecret or Secret Manager CSI driver in GKE

### Per service
- [ ] Kubernetes Deployment + Service manifest in `k8s/base/`
- [ ] Kustomize overlay for dev and prod in `k8s/overlays/`
- [ ] ArgoCD Application manifest pointing to overlay

### GitHub setup
- [ ] GitHub Environment `dev` (no approval) and `prod` (requires approval)
- [ ] Store GCP Workload Identity credentials in GitHub repo secrets
- [ ] Wire up `pr.yml` and `main.yml` pipelines

---

## Cost Estimate (approximate, us-central1)

| Resource | Dev (single project) | Prod (single project) |
|---|---|---|
| GKE Autopilot | ~$0.10/vCPU-hr (pay per pod) | ~$0.10/vCPU-hr |
| Cloud SQL (db-f1-micro) | ~$7/month | ~$25/month (db-g1-small) |
| Cloud Storage | ~$0.02/GB/month | ~$0.02/GB/month |
| Artifact Registry | ~$0.10/GB/month | shared |
| Load Balancer | ~$18/month | ~$18/month |
| **Rough total** | **~$30-50/month** | **~$60-100/month** |

Separate projects doubles the Load Balancer and cluster base cost but isolates billing clearly.
