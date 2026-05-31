<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# SkinAI (Comprehensive-design-frontend)

## Purpose
SkinAI is a full-stack skin-analysis application. A user captures a face image, the AI server runs skin classification models (age, pigment, wrinkle, homogeneity/sagging) and an LLM beauty-consultation agent, the backend persists results and handles auth + hospital search, and the frontend presents analysis, chat, history, and a hospital map. Prometheus + Grafana provide observability. All services are wired together with `docker-compose.yml`.

> Note: the repository folder is named `Comprehensive-design-frontend`, but it is the **whole** SkinAI monorepo (frontend + backend + AI + monitoring + k8s), not just the frontend.

## Architecture
```text
Browser ──> frontend (Vue 3 / Vite, :5173)
                 │  /api proxy
                 ▼
            backend (Spring Boot, :8080) ──> MariaDB (auth), MongoDB (results/chat), Redis (tokens)
                 │  proxies /api/ai/*, /api/analyses, /api/consultations
                 ▼
            ai (FastAPI, :8000) ──> classification models + beauty-agent (OpenAI) + AuraDB rules
                 │
            Prometheus (:9090) scrapes backend + ai ──> Grafana (:3000) dashboards
```

## Key Files
| File | Description |
|------|-------------|
| `docker-compose.yml` | Orchestrates all services: redis, mongo, mariadb, ai, backend, frontend, prometheus, grafana |
| `Readme.md` | Korean operator guide: run order, ports, env vars, API list, verification commands |
| `.env.example` | Template for root `.env` (Grafana admin credentials) |
| `.env` | Local (uncommitted) environment values |
| `.editorconfig` | Editor formatting conventions |
| `.gitignore` | Ignore rules |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `frontend/` | Vue 3 + Vite SPA — capture, analysis result, chat, history, hospital map (see `frontend/AGENTS.md`) |
| `backend/` | Spring Boot API — JWT auth, AI proxy, result/chat persistence, hospital search (see `backend/AGENTS.md`) |
| `AI/` | FastAPI AI server — skin classification, beauty-consultation agent, inference pipeline (see `AI/AGENTS.md`) |
| `monitoring/` | Prometheus scrape config + Grafana datasource and dashboards (see `monitoring/AGENTS.md`) |
| `k8s/` | Kubernetes manifests (kustomize) mirroring the compose stack (see `k8s/AGENTS.md`) |

## Service Map
| Service | Tech | Port | Notes |
|---------|------|------|-------|
| frontend | Vue 3 + Vite | 5173 | Proxies `/api` to backend |
| backend | Spring Boot | 8080 (mgmt 8081) | JWT auth, AI proxy, persistence |
| ai | FastAPI | 8000 | Classification + LLM agent |
| redis | Redis 7 | internal | Refresh-token store |
| mongo | MongoDB 7 | internal | Analysis results + chat history |
| mariadb | MariaDB 11 | internal | `skinai_auth` user DB |
| prometheus | Prometheus 2.55 | 9090 | Scrapes backend + ai |
| grafana | Grafana 11.4 | 3000 | 3 provisioned dashboards |

## For AI Agents

### Working In This Directory
- This is a polyglot monorepo: Python (AI), Java/Gradle (backend), TypeScript/Vue (frontend). Use the right toolchain per area — see each area's `AGENTS.md`.
- Secrets live in per-service `.env` files (`backend/.env`, `AI/.env`, `frontend/.env`) and the root `.env`. Never commit secrets; `.env.example` files show the expected keys.
- Cross-service contracts: the API surface is listed in `Readme.md` ("주요 API") and `frontend/apiList.md`. Changing a route requires updating both the backend controller and the frontend `src/lib/api.ts`.
- File-size limits matter: backend multipart and codec limits are set to 10MB in `docker-compose.yml`; image uploads above that fail with 413.

### Testing / Verification Requirements
- Full stack: `docker compose up -d --build` then `docker compose ps`; verify `curl http://localhost:8000/health`, `curl http://localhost:8080/api/ai/health`, `curl http://localhost:9090/-/ready`.
- Frontend: `cd frontend; corepack pnpm lint; corepack pnpm build`.
- Backend: `cd backend; .\gradlew.bat compileJava; .\gradlew.bat test`.
- AI: `python -m py_compile AI\api.py` (and other entrypoints).
- Compose config: `docker compose config --quiet`.

### Common Patterns
- Backend acts as a gateway to the AI server (`AI_SERVICE_URL=http://ai:8000`); the frontend never calls the AI server directly.
- Observability is opt-in via Spring Actuator (`/actuator/prometheus`) and AI `/metrics`.

## Dependencies

### External
- Docker + Docker Compose (orchestration)
- OpenAI API (beauty-consultation agent)
- Kakao Local API (hospital search), Kakao Maps JS SDK (frontend map)
- Neo4j AuraDB (beauty-rule knowledge base used by the AI agent)

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
