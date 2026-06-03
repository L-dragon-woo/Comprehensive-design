# SkinAI EKS Monitoring Runbook

## Scope

This runbook covers the production-style monitoring path:

- Prometheus scrapes SkinAI service metrics.
- Grafana provisions dashboards from repository JSON.
- CloudWatch remains the AWS-native source for EKS node, RDS, ElastiCache, load balancer, and log evidence.

## Deploy

```bash
kubectl apply -k k8s/overlays/eks --load-restrictor=LoadRestrictionsNone
kubectl rollout status deploy/prometheus -n skinai
kubectl rollout status deploy/grafana -n skinai
kubectl rollout status deploy/backend -n skinai
kubectl rollout status deploy/ai -n skinai
```

## Kubernetes Checks

```bash
kubectl get pods -n skinai
kubectl get svc -n skinai prometheus grafana backend ai
kubectl get pvc -n skinai prometheus-data grafana-data
```

Expected:

- `prometheus` is `1/1 Running`.
- `grafana` is `1/1 Running`.
- `backend` exposes service ports `8080` and `8081`.
- `prometheus-data` and `grafana-data` are `Bound`.

## Prometheus Checks

Port-forward:

```bash
kubectl port-forward -n skinai svc/prometheus 9090:9090
```

Open:

```text
http://localhost:9090/targets
```

Expected targets:

- `prometheus` UP
- `backend` UP at `/actuator/prometheus`
- `ai` UP at `/metrics`

Useful queries:

```promql
up
http_server_requests_seconds_count{job="backend"}
ai_inference_count_total
process_resident_memory_bytes
```

## Grafana Checks

Port-forward:

```bash
kubectl port-forward -n skinai svc/grafana 3000:3000
```

Open:

```text
http://localhost:3000
```

Credentials come from the monitoring `grafana-admin` Secret:

```bash
kubectl exec -n skinai deploy/grafana -- printenv GF_SECURITY_ADMIN_USER
kubectl get secret -n skinai grafana-admin -o jsonpath='{.data.GF_SECURITY_ADMIN_PASSWORD}' | base64 -d
```

Expected:

- Datasource: `Prometheus`
- Folder: `SkinAI`
- Dashboards:
  - `Dashboard 1: System`
  - `Dashboard 2: Backend`
  - `Dashboard 3: AI`

## CloudWatch Checks

Use CloudWatch for AWS-managed infrastructure and incident evidence:

- EKS node health and pod restart events
- RDS CPU and connection count
- ElastiCache memory and connection count
- Application/load balancer request and target health metrics
- Centralized container logs, if Container Insights or log collection is enabled

Minimum incident triage:

```bash
kubectl get events -n skinai --sort-by=.lastTimestamp
kubectl logs -n skinai deploy/backend --tail=200
kubectl logs -n skinai deploy/ai --tail=200
kubectl get pods -n skinai
```

Then correlate timestamps with CloudWatch metrics for RDS, ElastiCache, EKS node status, and load balancer target health.

## Smoke Test

Run one image analysis request from the browser. Confirm:

- Prometheus `up{job="backend"}` and `up{job="ai"}` are `1`.
- Backend dashboard shows API request count and latency.
- AI dashboard shows inference count and duration after analysis.
- CloudWatch has no simultaneous RDS, ElastiCache, node, or load balancer alarms.
