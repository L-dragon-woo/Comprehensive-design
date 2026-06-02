# SkinAI EKS CI/CD and Production Architecture Plan

이 문서는 SkinAI 프로젝트의 현재 CI/CD, MongoDB Atlas, ECR, EKS 전환 내역과, 비용 제약 없이 실무형 운영 환경으로 확장할 때의 권장 아키텍처를 정리한다.

## 1. 현재까지 변경 내역

### 1.1 GitHub Actions CI

Jenkins 없이 GitHub Actions 기반 CI를 구성했다.

- Workflow: `.github/workflows/ci.yml`
- Trigger:
  - Pull request
  - `dev` branch push
  - Manual dispatch
- 검증 범위:
  - Docker Compose config validation
  - AI Python entrypoint compile
  - Backend Gradle compile/test with Redis, MariaDB, MongoDB service containers
  - Frontend pnpm lint/build
- 목적:
  - 배포 전 최소 품질 검증
  - PR/push 단위 자동 검증
  - Jenkins 없이 GitHub 저장소 중심 CI 운영

### 1.2 MongoDB Atlas 전환

MongoDB는 Kubernetes 내부 운영보다 Atlas로 분리하는 방향을 선택했다.

- 문서: `docs/mongodb-atlas-migration.md`
- Root `.env`에서 `SPRING_DATA_MONGODB_URI`를 주입하도록 정리
- Docker Compose backend 환경변수도 Atlas URI를 받을 수 있도록 변경
- EKS overlay에서는 local MongoDB manifest를 제외하고, Atlas URI를 Kubernetes Secret에서 주입

선택 이유:

- MongoDB 운영 부담 감소
- EKS 내부 stateful workload 감소
- 백업, 복제, 모니터링, 확장성을 Atlas에 위임
- 애플리케이션 배포와 데이터베이스 운영 관심사 분리

### 1.3 ECR 구성

서비스별 ECR private repository를 생성했다.

- `skinai-frontend`
- `skinai-backend`
- `skinai-ai`

GitHub Actions CD에서 각 이미지를 빌드하고 commit SHA 기반 immutable tag로 push한다.

### 1.4 EKS 클러스터 구성

EKS Auto Mode 기반 클러스터를 생성했다.

- Cluster: `skinai-cluster`
- Region: `ap-northeast-2`
- Kubernetes version: `1.35`
- EKS Auto Mode enabled
- Cluster role: `skinai-eks-cluster-role`
- Node role: `skinai-eks-node-role`

현재는 빠른 포트폴리오 검증과 비용 관리를 위해 Auto Mode 기반으로 시작했다.

### 1.5 GitHub OIDC 기반 AWS 인증

장기 AWS access key를 GitHub Secrets에 저장하지 않고, OIDC로 IAM Role을 assume하도록 구성했다.

- IAM Role: `github-actions-skinai-eks-role`
- GitHub OIDC provider: `token.actions.githubusercontent.com`
- Trust policy scope:
  - Repository: `L-dragon-woo/Comprehensive-design`
  - Branch: `dev`
- Role permissions:
  - ECR image push/pull
  - EKS cluster describe
  - EKS access entry를 통한 Kubernetes admin access

보안상 장점:

- 장기 access key 불필요
- GitHub branch/repo 조건으로 assume role 제한 가능
- 권한 회전 부담 감소

### 1.6 EKS CD Workflow

EKS 배포 workflow를 추가했다.

- Workflow: `.github/workflows/deploy-eks.yml`
- 현재 Trigger: manual `workflow_dispatch`
- Flow:
  - Checkout
  - Assume AWS role through OIDC
  - Login to ECR
  - Build and push frontend/backend/AI images
  - Update kubeconfig
  - Upsert runtime Kubernetes Secret
  - Render `k8s/overlays/eks`
  - Replace placeholder image tags with immutable ECR tags
  - Apply manifests
  - Wait for rollout

첫 수동 배포를 안정화한 뒤 CI 성공 후 자동 CD로 전환할 수 있다.

### 1.7 EKS Kubernetes Overlay

EKS 전용 kustomize overlay를 구성했다.

- Path: `k8s/overlays/eks`
- MongoDB manifest 제외
- Atlas URI를 `skinai-secret`으로 주입
- Frontend/backend 2 replicas
- Rolling update:
  - `maxUnavailable: 0`
  - `maxSurge: 1`
- PodDisruptionBudget:
  - frontend
  - backend
- Backend readiness/liveness probe를 actuator HTTP endpoint로 변경
- Frontend Service를 LoadBalancer로 변경
- EKS Auto Mode EBS StorageClass 추가:
  - `auto-ebs-sc`
  - provisioner: `ebs.csi.eks.amazonaws.com`

### 1.8 현재 확인된 배포 상태와 이슈

현재까지 확인된 상태:

- ECR image build/push 성공
- Frontend Pod Running 확인
- Manifests apply 성공
- Backend CrashLoopBackOff 원인 확인:
  - MariaDB Pod가 Pending이라 backend가 DB 연결 실패
- AI/MariaDB/Redis Pending 원인 확인:
  - PVC에 StorageClass가 없었음

수정 방향:

- `k8s/overlays/eks/storage-class.yaml` 추가
- PVC에 `storageClassName: auto-ebs-sc` 패치
- 기존 `<unset>` PVC 삭제 후 새 workflow 실행 필요

## 2. 현재 아키텍처

```text
Developer
  -> GitHub push / workflow_dispatch
  -> GitHub Actions
      -> CI
      -> Build Docker images
      -> Push to ECR
      -> kubectl apply to EKS

User
  -> Frontend LoadBalancer
  -> frontend Pod
  -> backend Service
  -> backend Pod
      -> MariaDB Pod
      -> Redis Pod
      -> MongoDB Atlas
      -> AI Service
  -> ai Pod
      -> OpenAI
      -> Neo4j AuraDB
```

현재 구조는 포트폴리오 관점에서 다음을 보여줄 수 있다.

- GitHub Actions 기반 CI/CD
- ECR 이미지 레지스트리
- EKS 기반 Kubernetes 배포
- Kustomize overlay
- GitHub OIDC 기반 AWS 인증
- MongoDB Atlas 외부 managed database 연동
- Rolling update 기반 무중단 배포 준비

## 3. 현재 구조의 한계

현재 구조는 학습/포트폴리오용으로 충분히 의미가 있지만, 실무 운영 기준에서는 다음 한계가 있다.

### 3.1 데이터 계층

- MariaDB와 Redis가 EKS 내부 Pod/PVC로 운영됨
- 장애, 백업, 복구, multi-AZ 운영, patching 책임이 애플리케이션 팀에 남음
- MariaDB는 Deployment보다 StatefulSet 또는 managed RDS가 적합

### 3.2 AI 서비스

- AI uploads가 ReadWriteOnce PVC에 묶여 있음
- AI replica를 2개 이상으로 늘리면 파일 공유 문제가 발생할 수 있음
- 완전한 무중단 배포를 위해 uploads/model artifacts를 S3/EFS로 분리해야 함

### 3.3 Frontend runtime

- 현재 frontend image가 Vite dev server 기반이면 production serving에는 부적합
- 실무에서는 정적 빌드 산출물을 Nginx, CloudFront, S3, 또는 containerized Nginx로 서빙하는 편이 안정적

### 3.4 외부 트래픽

- 현재는 frontend Service LoadBalancer 중심
- 실무에서는 ALB Ingress, TLS, WAF, Route 53, CloudFront를 사용하는 구조가 일반적

### 3.5 Secrets 운영

- GitHub Secrets에서 Kubernetes Secret을 생성하는 방식은 시작 단계에는 괜찮음
- 실무에서는 AWS Secrets Manager 또는 External Secrets Operator로 이전하는 것이 좋음

### 3.6 배포 전략

- 현재는 rolling update 중심
- 실무에서는 canary, blue/green, progressive delivery, 자동 rollback까지 고려

## 4. 비용 제약 없는 실무형 목표 아키텍처

비용보다 안정성, 보안, 확장성, 운영성을 우선한다면 다음 구조를 목표로 한다.

```text
Users
  -> Route 53
  -> CloudFront
  -> AWS WAF
  -> ALB Ingress Controller
  -> EKS Services
      -> frontend
      -> backend
      -> ai

Data / External Services
  -> RDS MariaDB or Aurora MySQL
  -> ElastiCache Redis
  -> MongoDB Atlas Private Endpoint
  -> S3 for uploads and model artifacts
  -> Neo4j AuraDB
  -> OpenAI API

Platform
  -> EKS managed node groups or EKS Auto Mode
  -> private subnets for workloads
  -> public subnets only for ALB/NAT
  -> GitHub Actions OIDC
  -> ECR
  -> External Secrets Operator
  -> CloudWatch / Prometheus / Grafana / OpenTelemetry
```

## 5. 실무형 네트워크 설계

### 5.1 VPC

권장 구성:

- Multi-AZ VPC
- Public subnets:
  - ALB
  - NAT Gateway
- Private subnets:
  - EKS nodes
  - RDS
  - ElastiCache
  - Internal services
- Database subnets:
  - RDS/Aurora 전용 subnet group
- VPC endpoints:
  - ECR API
  - ECR DKR
  - S3
  - CloudWatch Logs
  - Secrets Manager
  - STS

목적:

- Pod가 public internet에 직접 노출되지 않음
- AWS 내부 서비스 통신 비용/보안 최적화
- NAT 의존도 감소

### 5.2 Ingress and Traffic

권장 트래픽 흐름:

```text
Route 53
  -> CloudFront
  -> WAF
  -> ALB
  -> Ingress
  -> frontend/backend services
```

권장 설정:

- TLS certificate: ACM
- HTTPS redirect
- WAF managed rules:
  - SQL injection
  - XSS
  - Known bad inputs
  - Bot control if needed
- ALB health check:
  - frontend: `/`
  - backend: `/actuator/health/readiness`
  - ai: `/health`
- Rate limiting:
  - WAF rate-based rule
  - Backend API gateway-level throttling if API Gateway is introduced

### 5.3 Internal API Routing

실무적으로는 frontend가 backend public API만 호출하고, AI는 backend 내부 API를 통해서만 호출되도록 한다.

- Public:
  - frontend
  - backend API
- Internal only:
  - AI service
  - Redis
  - Database

AI가 public internet에 노출되지 않도록 Service는 ClusterIP로 유지한다.

## 6. 실무형 보안 설계

### 6.1 IAM

권장 원칙:

- GitHub Actions는 OIDC assume role 사용
- Role trust policy는 repo/branch/environment로 제한
- ECR push 권한과 EKS deploy 권한 최소화
- 운영 환경과 개발 환경의 AWS role 분리

예시:

- `github-actions-skinai-dev-deploy-role`
- `github-actions-skinai-prod-deploy-role`
- `skinai-backend-irsa-role`
- `skinai-ai-irsa-role`

### 6.2 Kubernetes RBAC

권장:

- GitHub Actions role에 cluster-admin을 장기적으로 부여하지 않음
- 배포 namespace 범위 Role/RoleBinding으로 축소
- 운영자/admin access와 CI deploy access 분리
- namespace:
  - `skinai-dev`
  - `skinai-staging`
  - `skinai-prod`

### 6.3 Secrets

현재:

- GitHub Secrets -> `kubectl create secret generic skinai-secret`

실무 권장:

- AWS Secrets Manager
- External Secrets Operator
- Secret rotation
- KMS encryption
- GitHub Secrets에는 최소한의 AWS OIDC 정보만 유지

목표 흐름:

```text
AWS Secrets Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod env / mounted secret
```

### 6.4 Pod Security

권장:

- Run as non-root
- Read-only root filesystem where possible
- Drop Linux capabilities
- Resource requests/limits 필수
- Pod Security Standards 적용
- NetworkPolicy 적용

예시 정책:

- backend는 MariaDB, Redis, Atlas, AI에만 egress
- AI는 OpenAI, Neo4j, S3에만 egress
- frontend는 backend에만 egress

### 6.5 Image Security

권장:

- ECR enhanced scanning
- Trivy image scan in CI
- SBOM 생성
- Base image 최소화
- Immutable image tag 사용
- `latest` 운영 배포 금지

## 7. 실무형 데이터 계층

### 7.1 MariaDB

현재:

- EKS 내부 MariaDB Pod + PVC

실무 권장:

- Amazon RDS MariaDB 또는 Aurora MySQL
- Multi-AZ
- Automated backup
- Point-in-time recovery
- Read replica if read traffic grows
- Private subnet only
- Security group restrict to backend only

### 7.2 Redis

현재:

- EKS 내부 Redis Pod + PVC

실무 권장:

- Amazon ElastiCache Redis
- Multi-AZ replication group
- Auto failover
- AUTH/TLS enabled
- Private subnet only
- Security group restrict to backend only

### 7.3 MongoDB

현재:

- MongoDB Atlas 사용

실무 권장:

- Atlas dedicated cluster
- Private endpoint / VPC peering
- IP allowlist 최소화
- Automated backups
- Monitoring alerts
- Database user least privilege

### 7.4 File Uploads and Model Artifacts

현재:

- AI uploads PVC

실무 권장:

- S3 for user uploads
- S3 or EFS for model artifacts
- CloudFront signed URL if direct download needed
- Object lifecycle policy
- Antivirus/malware scanning pipeline if user-uploaded files are accepted

이렇게 바꾸면 AI replicas를 2개 이상으로 늘릴 수 있고, AI도 무중단 배포가 가능해진다.

## 8. 실무형 배포 전략

### 8.1 Environments

권장:

- `dev`
- `staging`
- `prod`

각 환경은 분리된 namespace 또는 분리된 cluster를 사용한다.

포트폴리오에서는 다음 정도만 구현해도 충분히 좋다.

- `dev`: 자동 배포
- `prod`: GitHub Environment approval 후 수동 승인 배포

### 8.2 CI Pipeline

권장 단계:

```text
lint
typecheck
unit test
integration test
docker build
image scan
SBOM
push to ECR
```

### 8.3 CD Pipeline

권장 단계:

```text
CI success
  -> deploy to staging
  -> smoke test
  -> manual approval
  -> deploy to prod
  -> rollout status
  -> post-deploy smoke test
  -> notify
```

### 8.4 Deployment Method

현재:

- GitHub Actions가 `kubectl apply` 실행

실무 권장:

- GitOps:
  - Argo CD 또는 Flux
  - GitHub Actions는 image build/push와 manifest update까지만 담당
  - Cluster apply는 Argo CD가 담당

장점:

- 배포 상태가 Git에 남음
- drift 감지 가능
- rollback이 쉬움
- cluster credential을 CI runner에 직접 많이 노출하지 않음

### 8.5 Rollback

현재 가능한 rollback:

```bash
kubectl rollout undo deploy/backend -n skinai
kubectl rollout undo deploy/frontend -n skinai
kubectl rollout undo deploy/ai -n skinai
```

실무 권장:

- Helm release rollback 또는 Argo CD rollback
- 이미지 태그는 commit SHA로 고정
- DB migration은 backward-compatible하게 설계
- 배포 후 smoke test 실패 시 자동 rollback

### 8.6 Zero-Downtime

현재:

- frontend/backend는 2 replicas + rolling update + readiness probe + PDB
- AI는 uploads PVC 때문에 1 replica

실무 목표:

- 모든 stateless service 2개 이상 replicas
- HPA 적용
- readiness probe가 true가 되기 전 트래픽 차단
- `maxUnavailable: 0`
- `maxSurge: 1`
- PodDisruptionBudget
- DB migration은 expand/contract 방식

## 9. 트래픽과 확장성

### 9.1 Horizontal Pod Autoscaler

권장:

- frontend:
  - CPU 기반 HPA
- backend:
  - CPU + memory + request latency 기반
- AI:
  - CPU/GPU or queue length 기반

### 9.2 Cluster Autoscaling

권장:

- EKS Auto Mode 또는 Karpenter
- workload별 node pool:
  - system
  - general-purpose
  - ai
- AI가 GPU를 사용한다면 GPU node pool 분리

### 9.3 Queue-Based Scaling

AI 분석이 오래 걸리면 동기 HTTP 처리보다 queue 기반 처리가 실무적으로 더 안정적이다.

권장 구조:

```text
backend
  -> SQS
  -> ai worker
  -> S3 result
  -> backend polling or WebSocket/SSE notification
```

장점:

- 트래픽 급증 시 완충
- AI worker 독립 확장
- timeout 문제 감소
- 재시도/실패 처리 쉬움

## 10. 관측성

현재:

- Prometheus/Grafana compose 기반 구성
- Backend actuator
- AI `/metrics`

실무 권장:

- Metrics:
  - Amazon Managed Prometheus 또는 in-cluster Prometheus
  - Amazon Managed Grafana
- Logs:
  - CloudWatch Container Insights
  - structured JSON logs
- Traces:
  - OpenTelemetry
  - AWS X-Ray or Tempo
- Alerts:
  - Pod crash
  - Deployment rollout failure
  - 5xx rate
  - p95/p99 latency
  - DB connection pool exhaustion
  - Redis memory usage
  - AI inference latency

핵심 SLO 예시:

- API availability: 99.9%
- Backend p95 latency: under 300ms for normal API
- AI analysis accepted latency: under 1s
- AI result completion: under target business SLA
- Error rate: under 1%

## 11. 운영 Runbook

### 11.1 배포 확인

```bash
kubectl get pods -n skinai
kubectl get deploy -n skinai
kubectl rollout status deploy/frontend -n skinai
kubectl rollout status deploy/backend -n skinai
kubectl rollout status deploy/ai -n skinai
```

### 11.2 PVC 확인

```bash
kubectl get storageclass
kubectl get pvc -n skinai
kubectl describe pvc mariadb-data -n skinai
```

### 11.3 로그 확인

```bash
kubectl logs deploy/backend -n skinai
kubectl logs deploy/ai -n skinai
kubectl get events -n skinai --sort-by=.lastTimestamp
```

### 11.4 외부 접속 확인

```bash
kubectl get svc frontend -n skinai
```

### 11.5 롤백

```bash
kubectl rollout history deploy/backend -n skinai
kubectl rollout undo deploy/backend -n skinai
```

## 12. 실무형 전환 로드맵

### Phase 1. 현재 EKS 배포 안정화

- EKS Auto Mode StorageClass 적용
- PVC Bound 확인
- MariaDB/Redis Running 확인
- Backend Running 확인
- AI Running 확인
- Frontend LoadBalancer 접속 확인

### Phase 2. 자동 CD 전환

- `Deploy to EKS`를 CI 성공 후 자동 실행
- GitHub Environment로 production approval 추가
- 실패 시 rollback 또는 알림 추가

### Phase 3. Managed Data 전환

- MariaDB -> RDS/Aurora
- Redis -> ElastiCache
- MongoDB Atlas private endpoint 구성
- EKS 내부 MariaDB/Redis 제거

### Phase 4. Production Traffic

- ALB Ingress Controller 또는 EKS Auto Mode ingress 구성
- Route 53 domain 연결
- ACM TLS 인증서 연결
- WAF 적용
- CloudFront 적용

### Phase 5. Security Hardening

- External Secrets Operator 도입
- Kubernetes RBAC 최소 권한화
- NetworkPolicy 적용
- Pod securityContext 적용
- ECR enhanced scanning / Trivy scan 추가

### Phase 6. Observability

- CloudWatch Container Insights
- Prometheus/Grafana 운영 환경 구성
- OpenTelemetry tracing
- Alert rules 추가

### Phase 7. AI Scalability

- AI uploads를 S3/EFS로 이전
- AI replicas 2 이상
- HPA 적용
- 필요 시 SQS 기반 async analysis pipeline 도입

## 13. 포트폴리오에 쓸 수 있는 설명

이 프로젝트는 단순 EC2 Docker Compose 배포에서 시작해 GitHub Actions CI, MongoDB Atlas, ECR, EKS 기반 CD로 확장했다. Jenkins 없이 GitHub Actions OIDC를 사용해 AWS IAM Role을 assume하고, commit SHA 기반 immutable image를 ECR에 push한 뒤 Kubernetes manifest를 적용한다.

Kubernetes 배포에서는 frontend/backend를 2 replicas로 구성하고 readiness probe, rolling update, PodDisruptionBudget을 적용해 무중단 배포의 기본 조건을 구성했다. MongoDB는 Atlas로 분리해 데이터베이스 운영 부담을 낮췄고, EKS Auto Mode 환경에서는 EBS StorageClass를 명시해 PVC 기반 workload가 정상적으로 스케줄링되도록 개선했다.

실무형 구조로는 RDS/Aurora, ElastiCache, Atlas private endpoint, S3/EFS, ALB Ingress, Route 53, ACM, WAF, External Secrets Operator, GitOps, OpenTelemetry를 도입해 보안, 트래픽 처리, 관측성, 롤백 가능성을 강화하는 방향으로 설계할 수 있다.

