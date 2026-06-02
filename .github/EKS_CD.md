# EKS CD Runbook

This repository now has a scaffold for EKS-based CD with Kubernetes rolling updates.

## Flow

```text
push dev
  -> CI
  -> Deploy to EKS
  -> build Docker images
  -> push images to ECR
  -> apply k8s/overlays/eks
  -> wait for rollout status
```

The workflow is `.github/workflows/deploy-eks.yml`.

## Required AWS Resources

- EKS cluster in `ap-northeast-2`
- ECR repositories:
  - `skinai-frontend`
  - `skinai-backend`
  - `skinai-ai`
- GitHub OIDC IAM role that can push to ECR and deploy to EKS

## Required GitHub Secrets

```text
AWS_ROLE_ARN
EKS_CLUSTER_NAME
JWT_SECRET
AUTH_ADMIN_USERNAME
AUTH_ADMIN_PASSWORD
SPRING_DATASOURCE_PASSWORD
MARIADB_ROOT_PASSWORD
MARIADB_PASSWORD
KAKAO_REST_API_KEY
SPRING_DATA_MONGODB_URI
OPENAI_API_KEY
NEO4J_URI
NEO4J_USERNAME
NEO4J_PASSWORD
```

`SPRING_DATA_MONGODB_URI` should be the MongoDB Atlas URI.

AI optional secrets can also be registered when those features are used:

```text
GOOGLE_API_KEY
PUBMED_API_KEY
NEO4J_DATABASE
AURA_API_CLIENT_ID
AURA_API_CLIENT_SECRET
AGENT_ID
OPENAI_EMBED_MODEL
OPENAI_EMBED_DIMENSIONS
EMBED_BATCH_SIZE
EMBED_MIN_BATCH_SIZE
EMBED_MAX_RETRIES
EMBED_RETRY_BASE_SECONDS
LANGCHAIN_API_KEY
LANGCHAIN_TRACING_V2
LANGCHAIN_PROJECT
LANGCHAIN_ENDPOINT
```

## Current Rollout Scope

The EKS overlay currently keeps `frontend` at 2 replicas and `backend` at 1 replica.
Backend is intentionally kept at 1 replica while the small RDS instance is connection-limited.

`frontend` uses:

```text
maxUnavailable: 0
maxSurge: 1
readinessProbe
PodDisruptionBudget
rollout status checks
```

When RDS capacity or connection pooling is ready for more traffic, raise backend replicas back to 2 and keep the same rolling update settings.

The AI service stays at 1 replica because it still uses a `ReadWriteOnce` uploads PVC. For full AI zero-downtime rollout, move uploads to S3 or EFS and then raise AI replicas to 2.

## Notes

The root `k8s/` manifests remain the local Kubernetes baseline. EKS uses `k8s/overlays/eks`, which excludes local MongoDB and injects the Atlas URI through `skinai-secret`.
