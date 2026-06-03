# SkinAI EKS 전환 트러블슈팅 기록

> 2026-06-03 기준 SkinAI를 EKS 기반 Cloud Native 구조로 옮기면서 겪은 문제와 해결 과정을 정리했다.

## 배경

SkinAI는 Vue 프론트엔드, Spring Boot 백엔드, FastAPI AI 서버로 구성된 피부 분석 서비스다.

이번 작업의 목표는 단순히 애플리케이션을 띄우는 것이 아니라, 실서비스 환경을 가정해 다음 요소를 붙이는 것이었다.

- S3 Presigned URL 기반 이미지 업로드
- EKS 기반 배포
- Prometheus + Grafana 모니터링
- CloudWatch 인프라 지표/알람
- HPA 기반 자동 확장
- GitHub Actions + ECR + EKS 자동 배포

작업 중 여러 문제가 연달아 발생했고, 각각의 원인을 확인하며 해결했다.

## 1. Presigned URL이 보이지 않던 문제

### 증상

브라우저 DevTools Network에서 이미지 분석 요청을 확인했을 때 처음에는 `presigned-upload` 요청이 보이지 않았다.

또는 분석 요청이 `/api/analyses`로 바로 가거나, `/api/analyses/from-s3`가 실패했다.

### 확인한 것

먼저 프론트엔드가 실제로 Presigned URL 요청을 보내는지 확인했다.

DevTools Network에서 기대한 흐름은 다음과 같다.

```text
POST /api/files/images/presigned-upload  200
PUT  https://S3_BUCKET/...               200
POST /api/analyses/from-s3               200
GET  /api/analyses/summary               200
```

이후 EKS의 백엔드 환경변수도 확인했다.

```bash
kubectl exec -n skinai deploy/backend -- printenv S3_BUCKET AWS_REGION S3_PRESIGNED_URL_TTL
```

정상 출력:

```text
skinai-s3-...
ap-northeast-2
PT1H
```

### 원인

초기에는 프론트엔드와 백엔드 배포 버전이 맞지 않았고, Presigned Upload 경로가 정상적으로 반영되지 않은 상태였다.

또한 S3 업로드 이후 `/api/analyses/from-s3`에서 AI 분석 또는 결과 저장 단계가 실패하면 사용자는 전체 분석 실패처럼 보게 되었다.

### 해결

프론트엔드에서 Presigned URL 요청을 우선 사용하도록 정리하고, 실패 시 multipart 업로드로 fallback할 수 있게 했다.

백엔드에서는 AI 분석 결과는 반환하되, 저장이나 S3 후처리 실패가 전체 분석 실패로 전파되지 않도록 예외 처리를 보강했다.

최종적으로 Network에서 다음 흐름을 확인했다.

```text
presigned-upload         200
S3 PUT                   200
/api/analyses/from-s3    200
/api/analyses/summary    200
```

### 배운 점

Presigned Upload는 프론트, 백엔드, S3 권한, 배포 버전이 모두 맞아야 정상 동작한다.

특히 Network 탭에서 실제 요청 순서를 확인하는 것이 가장 빠른 진단 방법이었다.

## 2. EKS Pod가 ContainerCreating/Terminating에 걸리던 문제

### 증상

EKS에 새 버전을 배포한 뒤 일부 Pod가 계속 `ContainerCreating` 또는 `Terminating` 상태에 머물렀다.

```bash
kubectl get pods -A
```

예시:

```text
ai-...        0/1   ContainerCreating
mariadb-...   0/1   ContainerCreating
redis-...     0/1   ContainerCreating
```

### 원인

PVC가 `ReadWriteOnce`로 붙어 있는 Deployment에 RollingUpdate가 적용되면서 기존 Pod와 신규 Pod가 동시에 같은 EBS 볼륨을 붙이려고 했다.

그 결과 EBS Multi-Attach 문제가 발생했다.

### 해결

PVC를 사용하는 Deployment는 RollingUpdate 대신 Recreate 전략을 사용하도록 변경했다.

대상:

- `ai`
- `mariadb`
- `redis`
- `mongo`

EKS overlay에서 AI Deployment 전략을 다시 RollingUpdate로 덮어쓰고 있던 부분도 함께 수정했다.

### 배운 점

`ReadWriteOnce` PVC를 사용하는 워크로드는 무조건 RollingUpdate가 안전한 것이 아니다.

상태 저장 볼륨을 단일 Pod만 사용할 수 있다면 `strategy.type: Recreate`가 더 안전하다.

## 3. Prometheus와 Grafana가 CrashLoopBackOff에 빠진 문제

### 증상

모니터링 스택을 추가한 뒤 `prometheus`와 `grafana`가 계속 CrashLoopBackOff가 되었다.

```bash
kubectl get pod -n skinai
```

예시:

```text
grafana-...      0/1   CrashLoopBackOff
prometheus-...   0/1   CrashLoopBackOff
```

### 원인

Prometheus와 Grafana가 각각 PVC에 데이터를 써야 하는데, EBS PVC가 root 소유로 붙으면서 컨테이너 기본 사용자 권한으로 쓰기 실패가 발생했다.

### 해결

Pod `securityContext.fsGroup`을 추가했다.

```yaml
securityContext:
  fsGroup: 65534
  fsGroupChangePolicy: OnRootMismatch
```

Prometheus는 `65534`, Grafana는 `472`를 사용했다.

수정 후:

```bash
kubectl get pod -n skinai
```

정상 상태:

```text
grafana-...      1/1   Running
prometheus-...   1/1   Running
```

### 배운 점

PVC를 쓰는 공식 이미지들은 컨테이너 내부 실행 UID가 정해져 있는 경우가 많다.

EKS + EBS PVC 환경에서는 `fsGroup` 설정이 없으면 쓰기 권한 문제로 CrashLoop가 날 수 있다.

## 4. CloudShell port-forward 후 localhost 접속이 안 되던 문제

### 증상

CloudShell에서 Prometheus port-forward를 실행했다.

```bash
kubectl port-forward -n skinai svc/prometheus 9090:9090
```

정상 출력:

```text
Forwarding from 127.0.0.1:9090 -> 9090
```

하지만 내 PC 브라우저에서 `http://localhost:9090/targets`에 접속하면 연결이 거부되었다.

### 원인

CloudShell의 `localhost`와 내 PC의 `localhost`는 다르다.

즉, CloudShell에서 port-forward를 열면 그 포트는 CloudShell 내부에서만 열린다.

### 해결

CloudShell 내부에서 `curl`로 확인했다.

```bash
curl http://127.0.0.1:9090/-/ready
curl -s http://127.0.0.1:9090/api/v1/targets | grep -o '"health":"[^"]*"'
```

정상 출력:

```text
Prometheus Server is Ready.
"health":"up"
"health":"up"
"health":"up"
```

Grafana도 동일하게 확인했다.

```bash
kubectl port-forward -n skinai svc/grafana 3000:3000
curl http://127.0.0.1:3000/api/health
```

정상 출력:

```json
{
  "database": "ok",
  "version": "11.4.0"
}
```

### 배운 점

CloudShell에서 port-forward한 서비스는 내 로컬 브라우저의 `localhost`로 볼 수 없다.

UI를 직접 보려면 로컬 PC에 AWS CLI와 kubeconfig를 설정하거나, CloudShell Preview를 사용해야 한다.

## 5. Grafana를 LoadBalancer로 임시 공개했지만 접속이 안 되던 문제

### 증상

Grafana UI를 브라우저에서 보기 위해 Service를 임시로 LoadBalancer로 바꿨다.

```bash
kubectl patch svc grafana -n skinai -p '{"spec":{"type":"LoadBalancer"}}'
```

ELB 주소는 생성되었다.

```bash
kubectl get svc grafana -n skinai
```

하지만 브라우저에서 접속하면 열리지 않았다.

### 확인한 것

DNS는 정상적으로 잡혔다.

```bash
getent hosts <grafana-elb-hostname>
```

Grafana Pod와 endpoint도 정상이었다.

```bash
kubectl get endpoints grafana -n skinai
kubectl logs -n skinai deploy/grafana --tail=100
```

Grafana 내부 API도 정상 응답했다.

```bash
curl http://127.0.0.1:3000/api/health
```

### 결론

Grafana 자체 문제는 아니었고, 임시 LoadBalancer 외부 접속 경로 문제였다.

최종적으로 LoadBalancer 노출은 비용과 보안상 닫았다.

```bash
kubectl patch svc grafana -n skinai -p '{"spec":{"type":"ClusterIP"}}'
```

### 배운 점

Grafana 같은 운영 도구를 인터넷에 직접 열어두는 것은 좋지 않다.

실무적으로는 다음 중 하나가 더 적절하다.

- 로컬 kubeconfig + port-forward
- VPN/Bastion 경유
- Grafana Cloud / Amazon Managed Grafana
- 내부 ALB + 인증

## 6. Kakao Map JavaScript 키가 배포 환경에서 누락된 문제

### 증상

병원 찾기 화면에서 다음 메시지가 표시되었다.

```text
카카오맵 JavaScript 키가 설정되지 않았습니다.
frontend/.env에 VITE_KAKAO_MAP_APP_KEY를 추가해 주세요.
```

### 확인한 것

EKS frontend Pod 환경변수에 Kakao 키가 없었다.

```bash
kubectl exec -n skinai deploy/frontend -- printenv | grep KAKAO
```

출력 없음.

ConfigMap에도 없었다.

```bash
kubectl get configmap -n skinai skinai-config -o yaml | grep KAKAO
```

출력 없음.

### 원인

Vite의 `import.meta.env.VITE_*` 값은 런타임이 아니라 빌드 시점에 정적 번들에 삽입된다.

따라서 EKS ConfigMap에 나중에 넣는 것만으로는 이미 빌드된 프론트엔드 화면이 바뀌지 않는다.

### 해결

프론트엔드 Dockerfile에 build arg를 추가했다.

```dockerfile
ARG VITE_KAKAO_MAP_APP_KEY=""
ENV VITE_KAKAO_MAP_APP_KEY=$VITE_KAKAO_MAP_APP_KEY
```

GitHub Actions 배포 workflow에서 GitHub Secret을 build arg로 넘기도록 했다.

```yaml
build-args: |
  VITE_KAKAO_MAP_APP_KEY=${{ secrets.VITE_KAKAO_MAP_APP_KEY }}
```

또한 `.env`가 Docker build context에 들어가지 않도록 `frontend/.dockerignore`에 추가했다.

```text
.env
.env*.local
```

### 배운 점

Vite 환경변수는 런타임 환경변수와 다르게 생각해야 한다.

브라우저 번들에 필요한 값은 이미지 빌드 시점에 주입해야 한다.

## 7. CloudWatch에서 S3 지표가 null처럼 보이던 문제

### 증상

CloudWatch Dashboard에 S3 지표를 넣으려고 했지만 `BucketSizeBytes`, `NumberOfObjects`만 보였고, 값이 즉시 잘 보이지 않았다.

### 원인

S3 기본 스토리지 지표는 실시간이 아니라 일 단위로 갱신된다.

요청 수, 에러율, latency 같은 지표는 S3 Request Metrics를 별도로 활성화해야 한다.

### 결정

발표용 CloudWatch Dashboard에서는 S3 위젯을 제외했다.

대신 즉시성이 있는 지표 중심으로 구성했다.

- RDS CPU
- RDS Connection
- RDS FreeStorageSpace
- Load Balancer RequestCount
- Load Balancer 5xx
- HealthyHostCount / UnHealthyHostCount
- EKS Control Plane 지표

### 배운 점

CloudWatch 지표는 서비스마다 갱신 주기와 활성화 조건이 다르다.

발표나 운영 대시보드에서는 즉시성이 낮은 지표를 무리하게 넣기보다, 의미 있는 지표만 선별하는 것이 낫다.

## 8. HPA 적용 전제 조건 확인

### 목표

Frontend와 Backend를 CPU/Memory 사용률 기반으로 자동 확장하고 싶었다.

### 구현

`autoscaling/v2` HPA를 추가했다.

```yaml
kind: HorizontalPodAutoscaler
metadata:
  name: frontend-hpa
spec:
  minReplicas: 2
  maxReplicas: 5
```

Backend:

```text
minReplicas: 1
maxReplicas: 4
CPU 65%
Memory 80%
```

Frontend:

```text
minReplicas: 2
maxReplicas: 5
CPU 60%
Memory 75%
```

### 확인

```bash
kubectl get hpa -n skinai
```

정상 출력:

```text
backend-hpa    Deployment/backend    cpu: 8%/65%, memory: 59%/80%    1    4    1
frontend-hpa   Deployment/frontend   cpu: 2%/60%, memory: 68%/75%    2    5    2
```

`TARGETS`가 `<unknown>`이 아니라 실제 값으로 표시되었다.

이는 metrics-server가 정상 동작하고 있다는 뜻이다.

### 중요한 결정

AI 서비스는 HPA 대상에서 제외했다.

현재 AI Pod는 `ReadWriteOnce` PVC를 사용하고 있기 때문에 replica를 늘리면 다시 EBS Multi-Attach 문제가 발생할 수 있다.

따라서 AI는 현재 다음 방식으로 운영한다.

- 단일 replica
- liveness/readiness probe 기반 self-healing
- 추후 S3-only 처리 또는 공유 스토리지 전환 후 HPA 적용

### 배운 점

HPA를 적용하려면 다음이 필요하다.

- metrics-server
- Pod resource requests
- scale-out 가능한 stateless 구조

모든 Deployment가 HPA 대상이 될 수 있는 것은 아니다.

상태 저장 볼륨을 사용하는 워크로드는 storage 구조를 먼저 확인해야 한다.

## 최종 구성

최종적으로 SkinAI는 다음 구조를 갖추게 되었다.

### Security

- JWT Blacklist
- Kubernetes Secret
- Backend/AI 직접 외부 노출 차단
- Presigned URL 기반 S3 직접 업로드

### High Availability

- Frontend 최소 2 replicas
- Backend HPA
- Readiness/Liveness Probe
- RollingUpdate
- PVC 워크로드 Recreate 전략

### Automation

- GitHub Actions CI/CD
- Docker image build
- ECR push
- Kustomize 기반 EKS 배포

### Observability

- Prometheus metrics scrape
- Grafana dashboard
- CloudWatch RDS/ALB/EKS 지표
- CloudWatch Alarm 상태 기반 장애 감지

### Performance

- S3 Presigned Upload
- Backend network I/O 감소
- HPA 기반 traffic 대응
- 향후 Queue 기반 AI Worker 확장 가능

## 마무리

이번 작업에서 가장 많이 느낀 것은, 클라우드 네이티브 전환은 단순히 컨테이너를 띄우는 일이 아니라는 점이다.

EKS에 배포하는 순간 다음 요소들이 모두 함께 맞아야 했다.

- 스토리지 접근 방식
- 배포 전략
- 환경변수 주입 시점
- 메트릭 수집 경로
- 외부 노출 방식
- 자동 확장 전제 조건

결과적으로 SkinAI는 단순 기능 구현을 넘어, 운영 환경을 고려한 Cloud Native 아키텍처로 확장되었다.
