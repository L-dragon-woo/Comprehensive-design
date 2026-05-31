<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# monitoring

## Purpose

Provides the full observability stack for SkinAI: Prometheus scrapes metrics from all three services (backend Spring Boot, AI FastAPI, and itself), and Grafana auto-provisions three dashboards from JSON definitions on startup. No manual Grafana setup is required — datasource and dashboard provider are wired via provisioning config.

## Key Files

| File | Description |
|------|-------------|
| prometheus/prometheus.yml | Scrape configuration: global interval 15s, three jobs — `prometheus` (self, :9090), `backend` (`/actuator/prometheus` on :8081), `ai` (`/metrics` on :8000) |
| grafana/provisioning/datasources/prometheus.yml | Registers Prometheus as the default Grafana datasource (`uid: prometheus`, proxy access to `http://prometheus:9090`) |
| grafana/provisioning/dashboards/skinai.yml | Dashboard provider named `SkinAI`: loads JSON files from `/var/lib/grafana/dashboards`, polling every 10 seconds, UI edits allowed |
| grafana/dashboards/system.json | Dashboard 1 — System: `up` stat panel (UP/DOWN per job), CPU timeseries (`process_cpu_seconds_total`), Memory timeseries (`process_resident_memory_bytes` + JVM heap for backend); refreshes every 10s |
| grafana/dashboards/backend.json | Dashboard 2 — Backend: API Count (`http_server_requests_seconds_count`, req/s by method+uri+status), API Latency (avg and max seconds by method+uri), Error Rate (5xx and 4xx ratio); all exclude `/actuator` URIs |
| grafana/dashboards/ai.json | Dashboard 3 — AI: inference_count (`ai_inference_count_total` rate by status), inference_duration (avg and p95 from `ai_inference_duration_seconds` histogram), GPU utilization (`ai_gpu_utilization_percent`); refreshes every 10s |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| prometheus/ | Contains `prometheus.yml` — the single scrape config file mounted into the Prometheus container |
| grafana/ | Contains all Grafana config: `provisioning/datasources/` for datasource registration, `provisioning/dashboards/` for the dashboard provider, and `dashboards/` for the three pre-built JSON dashboard definitions |

## For AI Agents

### Working In This Directory

- Prometheus scrape targets use Docker Compose service names as hostnames (`backend`, `ai`, `prometheus`). If adding a new scrape target, use the Compose service name and verify the metrics path matches what the service exposes.
- The backend metrics port is **8081** (not 8080) — the Spring Boot actuator is configured on management port 8081.
- Dashboard JSON files use `"uid": "prometheus"` to reference the datasource, matching the `uid` field in `provisioning/datasources/prometheus.yml`. Never change the datasource UID without updating all three dashboard JSON files.
- Grafana reloads dashboards from disk every 10 seconds (`updateIntervalSeconds: 10`), so changes to dashboard JSON files take effect quickly without restarting the container.
- All three dashboards are tagged `["skinai", "<type>"]` and placed in the `SkinAI` folder in Grafana.

### Testing Requirements

- Verify Prometheus scrape config syntax: `promtool check config prometheus/prometheus.yml`
- After editing a dashboard JSON, confirm it is valid JSON (no trailing commas, correct bracket nesting) before committing — Grafana silently skips malformed files.
- Grafana provisioning YAML follows the Grafana provisioning API schema; invalid keys cause silent failures at startup.

### Common Patterns

- All dashboard panels set `"datasource": { "type": "prometheus", "uid": "prometheus" }` — use this exact object when adding new panels.
- Metric expressions exclude actuator endpoints with `uri!~"/actuator.*"` to keep backend dashboards focused on application traffic.
- Error rate panels use `clamp_min(..., 1)` to avoid division-by-zero when traffic is zero.
- Histogram percentiles use `histogram_quantile(0.95, sum by (le) (rate(..._bucket[5m])))` — follow this pattern for any new histogram metric.

## Dependencies

### Internal

- `../docker-compose.yml` — mounts `prometheus/prometheus.yml` into the Prometheus container and mounts `grafana/` directories into the Grafana container; sets service hostnames that appear in scrape targets.
- `../backend` — exposes `/actuator/prometheus` on port 8081 (Spring Boot management port).
- `../ai` — exposes `/metrics` on port 8000 (FastAPI + prometheus-client).

### External

- Prometheus (Docker image `prom/prometheus`) — time-series metrics store, port 9090.
- Grafana (Docker image `grafana/grafana`) — visualization layer, port 3000; reads provisioning config and dashboard JSON at startup.

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
