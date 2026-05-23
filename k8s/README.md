# Kubernetes 실행

이 매니페스트는 기존 `docker-compose.yml` 구성을 Kubernetes로 옮긴 기본 로컬 배포 구성입니다.

## 1. 이미지 빌드

Docker Desktop Kubernetes를 쓰는 경우 현재 Docker 이미지가 그대로 보입니다.

```powershell
docker compose build
```

Minikube를 쓰는 경우 Minikube Docker daemon에서 빌드하세요.

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

Docker Desktop Kubernetes에서 `localhost:30173`이 바로 열리지 않으면 포트 포워딩을 사용하세요.

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

## 5. 삭제

애플리케이션만 삭제:

```powershell
kubectl delete -k k8s
```

PVC까지 삭제하면 MongoDB/Redis/AI 업로드 데이터도 삭제됩니다.

```powershell
kubectl delete pvc -n skinai mongo-data mariadb-data redis-data ai-uploads
```
