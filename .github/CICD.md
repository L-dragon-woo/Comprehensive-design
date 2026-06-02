# GitHub Actions CI/CD

This repository can run CI without Jenkins. GitHub Actions provides the runner and checks each service on pull requests and pushes.

## Workflows

| Workflow | File | When it runs | Purpose |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Pull requests, pushes to `main`, `master`, `develop`, manual run | Frontend typecheck/build, backend compile/test with Redis/MariaDB/MongoDB, AI Python syntax compile, Docker Compose config validation |

## CD Later

CD is intentionally not configured yet because ECR/ECS has not been provisioned.

When AWS is ready, add a separate deployment workflow that:

- Uses GitHub OIDC instead of long-lived AWS access keys
- Pushes images to ECR repositories
- Updates ECS services or task definitions
- Runs only after CI succeeds
