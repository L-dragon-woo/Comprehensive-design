# Kubernetes 실행

이 디렉터리는 프론트엔드, 백엔드, Redis, MariaDB, MongoDB를 Kubernetes에 배포하는 기본 매니페스트입니다.

AI 서비스는 이 레포에서 배포하지 않습니다. 백엔드는 `skinai-config` ConfigMap의 `AI_SERVICE_URL` 값으로 별도 AI 서비스에 연결합니다.

## 1. 이미지 빌드

Docker Desktop Kubernetes를 쓰는 경우 현재 Docker 이미지가 그대로 보입니다.

```powershell
docker compose build
```

Minikube를 쓰는 경우 Minikube Docker daemon에서 빌드합니다.

```powershell
minikube docker-env | Invoke-Expression
docker compose build
```

## 2. 배포

```powershell
kubectl apply -k k8s
kubectl get pods -n skinai -w
```

## 3. 접속

Frontend는 NodePort `30173`으로 열립니다.

```text
http://localhost:30173
```

Docker Desktop Kubernetes에서 `localhost:30173`이 바로 열리지 않으면 포트 포워딩을 사용합니다.

```powershell
kubectl port-forward -n skinai svc/frontend 5173:5173
```

포트 포워딩 후:

```text
http://localhost:5173
```

## 4. 상태 확인

```powershell
kubectl get all -n skinai
kubectl get pvc -n skinai
kubectl logs -n skinai deploy/backend
```

## 5. AI 서비스 주소 변경

기본값은 `k8s/configmap.yaml`에 있습니다.

```yaml
AI_SERVICE_URL: http://ai-service:8000
```

AI 레포에서 배포하는 Service 이름이나 외부 URL에 맞게 이 값을 바꾼 뒤 다시 적용합니다.

```powershell
kubectl apply -k k8s
kubectl rollout restart deploy/backend -n skinai
```

## 6. 삭제

애플리케이션만 삭제:

```powershell
kubectl delete -k k8s
```

PVC까지 삭제하면 MongoDB, MariaDB, Redis 데이터도 삭제됩니다.

```powershell
kubectl delete pvc -n skinai mongo-data mariadb-data redis-data
```
