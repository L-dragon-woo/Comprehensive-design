<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# k8s

## Purpose

Kubernetes manifests that reproduce the full SkinAI docker-compose stack in a local Kubernetes cluster (Docker Desktop or Minikube). All resources live in the `skinai` namespace. `kustomization.yaml` ties every manifest together so the entire application deploys with a single `kubectl apply -k k8s` command.

## Key Files

| File | Description |
|------|-------------|
| kustomization.yaml | Kustomize entry point — lists all nine resource files in dependency order: namespace → configmap → secret → redis → mongo → mariadb → ai → backend → frontend |
| namespace.yaml | Declares the `skinai` namespace; must be applied first so all other resources resolve their `namespace: skinai` field |
| configmap.yaml | `skinai-config` ConfigMap — non-secret environment variables shared across services: datasource URLs (MariaDB, MongoDB, Redis), JWT settings (issuer, TTLs), AI service URL, and Vite proxy target |
| secret.yaml | `skinai-secret` Opaque Secret — sensitive values: `JWT_SECRET`, admin credentials, MariaDB passwords, and `KAKAO_REST_API_KEY`; placeholder values must be replaced before production use |
| ai.yaml | FastAPI AI service: 2 Gi PVC (`ai-uploads` at `/app/uploads`), Deployment using image `comprehensive-design-ai:latest` on port 8000, health probes at `/health`, ClusterIP Service |
| backend.yaml | Spring Boot backend: Deployment using image `comprehensive-design-backend:latest` on port 8080, `envFrom` both `skinai-config` and `skinai-secret`, TCP socket probes, ClusterIP Service |
| frontend.yaml | Vue frontend: Deployment using image `comprehensive-design-frontend:latest` on port 5173, `envFrom` `skinai-config`, HTTP probes at `/`, NodePort Service exposing port 30173 |
| mariadb.yaml | MariaDB 11: 5 Gi PVC (`mariadb-data`), Deployment with `MARIADB_DATABASE=skinai_auth`, passwords from `skinai-secret`, health probes via `healthcheck.sh`, ClusterIP Service on 3306 |
| mongo.yaml | MongoDB 7: 5 Gi PVC (`mongo-data`), health probes via `mongosh --eval "db.adminCommand('ping').ok"`, ClusterIP Service on 27017 |
| redis.yaml | Redis 7 Alpine: 1 Gi PVC (`redis-data`), AOF persistence enabled (`--appendonly yes`), health probes via `redis-cli ping`, ClusterIP Service on 6379 |
| README.md | Deployment runbook (Korean): image build steps for Docker Desktop and Minikube, `kubectl apply -k k8s` deploy command, NodePort access at `localhost:30173`, port-forward fallback, status check commands, and teardown including PVC deletion |

## For AI Agents

### Working In This Directory

- The frontend is the only NodePort service (port 30173). All other services use ClusterIP and communicate via Kubernetes DNS service names (matching the service `metadata.name` fields: `backend`, `ai`, `redis`, `mariadb`, `mongo`).
- Images are expected to be pre-built locally with `docker compose build`. All three app images use `imagePullPolicy: IfNotPresent`, so they must exist in the local Docker daemon before deploying.
- `configmap.yaml` references service names as hostnames (e.g., `http://ai:8000`, `redis://redis:6379`). These match the Kubernetes Service names and mirror the docker-compose service names exactly.
- `secret.yaml` contains placeholder credentials (`change-this-secret-to-a-long-random-value`, `admin1234`). These are intentionally weak for local dev; never commit production secrets here.
- When adding a new service, register it in `kustomization.yaml` resources list and ensure its namespace is `skinai`.

### Testing Requirements

- Validate kustomize output before applying: `kubectl kustomize k8s` (renders all manifests to stdout without applying).
- Verify all pods reach `Running` state: `kubectl get pods -n skinai -w`.
- Check PVC binding: `kubectl get pvc -n skinai` — all should show `Bound` status before the dependent pods start.
- Backend readiness uses a TCP socket probe (port 8080); logs are the primary debugging tool: `kubectl logs -n skinai deploy/backend`.

### Common Patterns

- Every manifest sets `namespace: skinai` explicitly — never omit this field.
- Stateful services (MariaDB, MongoDB, Redis, AI uploads) each define a PVC and a matching `volumes` + `volumeMounts` pair in the Deployment spec.
- Secrets are injected via `secretKeyRef` (individual env vars for MariaDB) or `secretRef` (bulk `envFrom` for backend). ConfigMap values use `configMapRef` via `envFrom` for backend and frontend.
- All Deployments define both `readinessProbe` and `livenessProbe`. Readiness uses shorter `initialDelaySeconds` than liveness to allow the kubelet time to start traffic only after a service is truly ready.
- The kustomize resource order in `kustomization.yaml` reflects startup dependencies: namespace and config first, data stores before application services.

## Dependencies

### Internal

- `../docker-compose.yml` — the docker-compose stack this mirrors; service names, ports, environment variables, and volume paths are kept in sync between both files.
- `../Dockerfile` (frontend), `../backend/Dockerfile`, `../ai/Dockerfile` — source images referenced as `comprehensive-design-frontend:latest`, `comprehensive-design-backend:latest`, `comprehensive-design-ai:latest`.

### External

- Kubernetes (Docker Desktop built-in or Minikube) — cluster runtime; kustomize support required (`kubectl` v1.14+).
- `mariadb:11`, `mongo:7`, `redis:7-alpine` — official upstream images pulled from Docker Hub by the cluster.

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
