# CLAUDE.md

This file provides repository-specific guidance for AI-assisted development in this project.

## Repository Overview

This repository contains the coursework project "今天吃什么 / WhatToEat".

| Directory | Tech | Purpose |
|---|---|---|
| `frontend/` | WeChat Mini Program | Main client application |
| `backend/` | Spring Boot 4 + Java 17 | REST API and persistence layer |
| `docs/` | Markdown + Mermaid | Architecture, database, API, and contribution docs |
| `docs/design/` | React + Vite + Tailwind CSS | UI prototype and design deliverables |

## Current Backend Status

The backend is no longer a scaffold-only project. It already contains:

- REST controllers
- application/domain service split
- Spring Data JPA repositories
- Flyway migration scripts
- `dev` and `test` profiles
- MySQL configuration for local development
- H2 in-memory database for tests and no-MySQL startup verification

## Commands

### Backend

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
cp .env.example .env
docker compose --env-file .env up --build
```

Notes:

- Default profile is `dev` and expects local MySQL on `localhost:3306/whattoeat_dev`
- `test` profile uses H2 and is the preferred mode when MySQL is not running
- Docker Compose uses the `docker` profile and starts MySQL plus backend together
- WeChat DevTools should call `http://127.0.0.1:8080`; real devices on the same LAN should call `http://<your-host-lan-ip>:8080`
- `.env.example` uses `AMAP_KEY=test-key`; replace it with a real key before testing restaurant/recommendation endpoints

### Frontend

- Open `frontend/` in WeChat DevTools
- Use WeChat DevTools for preview, debugging, and packaging

### UI Prototype

```bash
cd docs/design
npm install
npm run dev
npm run build
```

## Backend Structure

```text
backend/src/main/java/com/zjgsu/whattoeat/
├── controller/          # REST endpoints
├── service/
│   ├── application/     # orchestration layer
│   └── domain/          # business rules
├── integration/amap/    # Amap API integration
├── repository/          # JPA repositories
├── model/entity/        # persistence entities
├── common/              # response, error, exception handling
└── config/              # configuration binding and HTTP client setup
```

## Project Rules

### Tech Rules

- Backend language and runtime: Java 17
- Backend framework: Spring Boot 4
- Persistence: Spring Data JPA + Hibernate
- Database: MySQL in `dev`, H2 in `test`
- Schema evolution: Flyway migrations under `backend/src/main/resources/db/migration/`

### Coding Rules

- Keep controller thin; business flow belongs in application/domain services
- Do not bypass repository or integration boundaries
- Keep API responses wrapped in the shared response model
- Prefer explicit exceptions and centralized error handling
- Keep documentation aligned with actual code and migration scripts

### Documentation Rules

- `docs/architecture.md` is the source of truth for backend architecture
- `docs/database.md` must stay aligned with Flyway SQL
- `docs/api.md` should reflect exposed backend endpoints
- Personal contribution records go under `docs/contributions/`

### Avoid

- Do not hardcode secrets into source files
- Do not make schema changes without a new migration
- Do not document planned behavior as if it were already implemented
- Do not change frontend conventions when only backend work is requested
