# ──────────────────────────────────────────────
#  Login System — Docker helpers
# ──────────────────────────────────────────────

.PHONY: up down down-clean build-backend

## Start full stack (build images first)
up:
	docker compose up --build

## Stop containers, keep volumes & images
down:
	docker compose down

## Stop containers AND prune build cache + dangling images + volumes
down-clean:
	docker compose down --volumes --remove-orphans
	docker builder prune -f
	docker image prune -f
	@echo "✓ Containers stopped, volumes removed, build cache cleared."

## Build backend JAR (run before docker compose if not using multi-stage)
build-backend:
	cd backend/auth-service && mvn clean install -DskipTests
