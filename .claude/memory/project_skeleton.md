# Project Skeleton (Original Architecture Scaffold)

> This file preserves the original high-level architecture scaffold used to bootstrap the project.
> For the current live architecture, see [architecture.md](./architecture.md).

## Backend
- Spring Boot
- Maven
- Layered architecture
- Stateless JWT auth
- API versioned: /api/v1

## Layers
- controller
- service
- repository
- security
- config
- exception
- dto
- entity

## Database
- PostgreSQL (prod)
- H2 (dev)

## Observability
- Spring Actuator
- Prometheus
- Grafana

## Frontend
- React
- Axios
- Context-based auth
