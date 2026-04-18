# AGENTS.md

This file is the project-level guidance for agents working in this repository.

## 1. Project overview

**WhatToEat / 今天吃什么** is a WeChat Mini Program for restaurant discovery and recommendation.

### Current stack
- **Frontend:** WeChat Mini Program (native)
- **Backend:** Spring Boot 4, Java 17, Spring Data JPA
- **Database:** MySQL 8 for dev / Docker, H2 for test
- **External services:**
  - Amap Web Service API for restaurant POI data
  - Internal `ai-service/` for review tagging and recommendation answers
- **Migrations:** Flyway

### Current implemented backend capabilities
- Auth: mock WeChat login, logout, current user
- Restaurants: nearby search, keyword search, enhanced sorting, review summary
- Recommendations: random, cards, AI ask, AI ask stream
- User data:
  - blacklist CRUD
  - notes CRUD
  - restaurant review CRUD
- Public review list and aggregated review summary

### Core business constraint
- Restaurant master data comes from **Amap POI**, not a local restaurant master table.
- Local DB stores only **user-side data and snapshots**:
  - blacklist
  - notes
  - review records
  - metric snapshots
  - reserved choice history

---

## 2. Repository layout

```text
WhatToEat/
├── frontend/                    # WeChat Mini Program
├── backend/                     # Spring Boot backend
│   └── src/main/java/com/zjgsu/whattoeat/
│       ├── controller/
│       ├── service/
│       │   ├── application/
│       │   └── domain/
│       ├── integration/
│       │   ├── amap/
│       │   ├── ai/
│       │   └── wechat/
│       ├── repository/
│       ├── model/entity/
│       ├── common/
│       └── config/
├── ai-service/                  # Internal AI service (FastAPI)
├── docs/                        # Architecture / API / DB / frontend integration docs
├── docker-compose.yml
└── README.md
```

---

## 3. Required reading before changes

Before changing code, always read the latest relevant docs and current branch state first.

### Minimum required reading
- `README.md`
- relevant files under `docs/`
  - `docs/architecture.md`
  - `docs/database.md`
  - `docs/api.md`
  - `docs/api.yaml`
  - `docs/frontend-ai-review-integration.md` when touching frontend/backend integration

### Additional required review rule
Before modifying code for a feature that is already being actively changed in the branch, **review the current committed and uncommitted changes in that area first**. Do not implement against stale assumptions.

In practice, inspect at least:
- current `git diff`
- related controllers / services / tests / docs already changed in this branch

---

## 4. Working rules

### General
- Keep diffs small, reviewable, and reversible.
- Prefer updating existing patterns over introducing new abstractions.
- Do not hardcode secrets.
- Verify before claiming completion.
- Keep docs, API contract, and implementation aligned.
- For local file inspection and edits in this repository, do not use filesystem-style MCP file tools. Use native shell reads/search (`rg`, `sed`, `cat`), `git diff`, and `apply_patch` so changes stay visible and reviewable in terminal output.

### Backend rules
- Keep controllers thin.
- Put orchestration in `service/application`.
- Put business rules in `service/domain`.
- Do not bypass repository or integration boundaries.
- All API responses must use the unified response model.
- Prefer explicit business exceptions and centralized error handling.

### Database rules
- Any schema change must go through Flyway migration.
- Do not manually mutate schema without a migration.
- Keep `docs/database.md` aligned with actual migration SQL.

### Frontend rules
- Do not change frontend conventions unless the task actually requires frontend work.
- When documenting frontend integration, document **current reality**, not intended future behavior.
- If backend contract changed, update frontend-facing docs accordingly.

### AI / summary rules
- AI-generated fields are enhancement-only, not source-of-truth fields.
- Public API behavior matters more than internal AI pipeline state.
- If AI state is not exposed publicly, document the externally visible contract clearly.
- Python tooling in this repo is managed with `uv`; when working in `ai-service/`, prefer `uv run`, `uv sync`, and the existing `pyproject.toml` / `uv.lock` workflow over ad-hoc `pip` usage.

---

## 5. Common commands

### Backend

Run backend in dev profile:
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
```

Run backend in test profile:
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

Run backend tests:
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
```

### AI service

Run AI service locally:
```bash
cd ai-service
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Run AI service tests:
```bash
cd ai-service
uv run --with pytest pytest -q
```

Python tooling note:
- Prefer `uv` for Python package management, virtualenv management, and command execution unless the project explicitly requires something else.

### Docker compose

Start full local stack:
```bash
docker compose up --build
```

### Frontend
- Open `frontend/` in WeChat DevTools
- Local backend URL for simulator: `http://127.0.0.1:8080`
- Real-device debugging should use host LAN IP, not `localhost`

---

## 6. Testing expectations

### Always run relevant tests after changes
- Backend changes: run targeted tests first, then `./mvnw test`
- AI service changes: run `uv run --with pytest pytest -q`
- Contract/doc changes affecting API semantics: ensure `docs/api.md` and `docs/api.yaml` stay aligned

### For bug fixes
- Add or update a regression test first when practical.
- Confirm the test fails before the fix when you are changing behavior.

### For integration-sensitive changes
If a change affects API semantics, try to verify all of:
- controller behavior
- service behavior
- docs consistency
- error / empty-state semantics

---

## 7. Current integration notes that matter

### Restaurant query contract
- Coordinates use **GCJ-02**.
- `restaurants/nearby` and `restaurants/search` support enhanced fields and sort values.
- For non-distance sorts, behavior is candidate-pool sorting rather than a guaranteed globally sorted Amap result set; docs must not overclaim.

### Review summary contract
- `review-summary` always returns `200 OK` for empty/no-snapshot cases.
- Public API may expose `aiTags=[]` and `aiSummary=null` even when internal AI pipeline has non-ready or failed state.
- If internal status is not exposed, document only the public-facing contract.

### Frontend integration reality
- The frontend currently mixes real backend integration and local fallback/mock behavior.
- When updating frontend integration docs, explicitly call out:
  - what is already truly integrated
  - what is still local/mock
  - what must be fixed before reliable end-to-end integration

---

## 8. Documentation sources of truth

- `docs/architecture.md` → backend architecture truth source
- `docs/database.md` → DB design and migration alignment
- `docs/api.md` → human-readable API contract
- `docs/api.yaml` → OpenAPI contract
- `docs/frontend-ai-review-integration.md` → frontend/backend integration guidance for review, summary, and AI recommendation flows

If implementation changes contract semantics, update docs in the same work.

---

## 9. Important reminders

- Current auth flow is mock-WeChat shaped, not real production WeChat auth.
- Amap remains the single source for restaurant base data.
- `user_choice_history` is still reserved and not part of the active recommendation path.
- The repository now includes an internal AI service; when touching recommendation/summary behavior, consider both `backend/` and `ai-service/`.
