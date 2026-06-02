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
```

`SPRING_DATA_MONGODB_URI` should be the MongoDB Atlas URI.

## Zero-Downtime Scope

The EKS overlay sets `frontend` and `backend` to 2 replicas with:

```text
maxUnavailable: 0
maxSurge: 1
readinessProbe
PodDisruptionBudget
rollout status checks
```

The AI service stays at 1 replica because it still uses a `ReadWriteOnce` uploads PVC. For full AI zero-downtime rollout, move uploads to S3 or EFS and then raise AI replicas to 2.

## Notes

The root `k8s/` manifests remain the local Kubernetes baseline. EKS uses `k8s/overlays/eks`, which excludes local MongoDB and injects the Atlas URI through `skinai-secret`.
