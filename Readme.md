# SkinAI 통합 프로젝트

얼굴 이미지 분석을 기반으로 피부 분석, AI 상담, 병원 검색, 모니터링 대시보드를 제공하는 통합 애플리케이션입니다.

## 구성

```text
.
├─ frontend/             Vue 3 + Vite 프론트엔드
├─ backend/              Spring Boot API 서버
├─ AI/                   FastAPI AI 분석/상담 서버
├─ monitoring/           Prometheus, Grafana 설정과 대시보드
├─ docker-compose.yml    전체 서비스 실행 설정
└─ Readme.md
```

## 사전 준비

- Docker Desktop
- Docker Compose

Docker Desktop을 먼저 실행한 뒤 아래 명령을 사용합니다.

## 전체 실행 순서

프로젝트 루트에서 실행합니다.

```powershell
cd C:\Users\qhtm0\Desktop\project\Comprehensive-design-frontend
```

Grafana 관리자 계정은 실행 환경에서 주입해야 합니다. 비밀번호는 저장소에 커밋하지 않습니다.

루트 `.env` 파일을 만들거나 현재 PowerShell 세션에 아래 변수를 설정합니다. 예시는 `.env.example`을 참고합니다.

```powershell
$env:GF_SECURITY_ADMIN_USER="admin"
$env:GF_SECURITY_ADMIN_PASSWORD="<secure-password>"
```

```powershell
docker compose up -d --build
```

상태 확인:

```powershell
docker compose ps
```

## 접속 주소

| Service | URL | Notes |
| --- | --- | --- |
| Frontend | http://localhost:5173 | 사용자 화면 |
| Backend | http://localhost:8080 | Spring Boot API |
| AI Server | http://localhost:8000 | FastAPI AI 서비스 |
| Prometheus | http://localhost:9090 | 메트릭 수집/쿼리 |
| Grafana | http://localhost:3000 | 대시보드 |

Grafana 로그인:

```text
ID: $env:GF_SECURITY_ADMIN_USER 값
PW: $env:GF_SECURITY_ADMIN_PASSWORD 값
```

앱 기본 로그인:

```text
ID: admin
PW: admin
```

## 정상 동작 확인

```powershell
curl http://localhost:8000/health
curl http://localhost:8080/api/ai/health
curl http://localhost:9090/-/ready
```

Prometheus target 확인:

```text
http://localhost:9090/targets
```

`ai`, `backend`, `prometheus`가 `UP`이면 정상입니다.

## Grafana 대시보드

Docker Compose 실행 시 Grafana에 아래 대시보드가 자동 생성됩니다.

| Dashboard | URL |
| --- | --- |
| Dashboard 1: System | http://localhost:3000/d/skinai-system/dashboard-13a-system |
| Dashboard 2: Backend | http://localhost:3000/d/skinai-backend/dashboard-23a-backend |
| Dashboard 3: AI | http://localhost:3000/d/skinai-ai/dashboard-33a-ai |

### 주요 PromQL

System:

```promql
up
```

```promql
sum by (job) (rate(process_cpu_seconds_total[5m]))
```

```promql
process_resident_memory_bytes
```

Backend:

```promql
sum by (method, uri, status) (rate(http_server_requests_seconds_count{job="backend",uri!~"/actuator.*"}[5m]))
```

```promql
sum by (method, uri) (rate(http_server_requests_seconds_sum{job="backend",uri!~"/actuator.*"}[5m]))
/
sum by (method, uri) (rate(http_server_requests_seconds_count{job="backend",uri!~"/actuator.*"}[5m]))
```

```promql
(
  sum(rate(http_server_requests_seconds_count{job="backend",status=~"5..",uri!~"/actuator.*"}[5m]))
  /
  clamp_min(sum(rate(http_server_requests_seconds_count{job="backend",uri!~"/actuator.*"}[5m])), 1)
) or vector(0)
```

AI:

```promql
sum by (status) (rate(ai_inference_count_total[5m]))
```

```promql
rate(ai_inference_duration_seconds_sum[5m])
/
clamp_min(rate(ai_inference_duration_seconds_count[5m]), 0.001)
```

```promql
ai_gpu_utilization_percent
```

`inference_duration`의 `avg`는 평균 AI 분석 시간이고, `p95`는 요청 95%가 그 시간 안에 끝났다는 의미입니다.

## 환경변수

필요하면 아래 파일을 생성하거나 수정합니다.

```text
backend/.env
AI/.env
frontend/.env
```

### backend/.env 예시

```env
JWT_SECRET=change-this-secret-to-a-long-random-value
JWT_ISSUER=skinai
JWT_ACCESS_TTL_SECONDS=1800
JWT_REFRESH_TTL_SECONDS=1209600
AUTH_ADMIN_USERNAME=admin
AUTH_ADMIN_PASSWORD=admin
KAKAO_REST_API_KEY=
```

### AI/.env 예시

```env
OPENAI_API_KEY=
AI_MODEL=openai:gpt-4o-mini
```

### frontend/.env 예시

```env
VITE_KAKAO_MAP_APP_KEY=
```

카카오맵을 브라우저에서 띄우려면 카카오 개발자 콘솔의 JavaScript 키 Web 플랫폼에 아래 도메인을 등록해야 합니다.

```text
http://localhost:5173
```

## 주요 API

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/refresh` | 토큰 재발급 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/ai/health` | AI 서버 상태 확인 |
| POST | `/api/analyses` | 이미지 분석 요청 |
| POST | `/api/consultations/messages` | AI 상담 메시지 전송 |
| GET | `/api/hospitals/search` | 카카오 병원 검색 |
| GET | `/actuator/prometheus` | Backend Prometheus 메트릭 |
| GET | `/metrics` | AI Prometheus 메트릭 |

## 개발 검증 명령

Frontend:

```powershell
cd frontend
corepack pnpm lint
corepack pnpm build
```

Backend:

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

AI:

```powershell
python -m py_compile AI\api.py
```

Docker Compose 설정 확인:

```powershell
docker compose config --quiet
```

## 종료

컨테이너만 종료:

```powershell
docker compose down
```

볼륨 데이터까지 삭제:

```powershell
docker compose down -v
```

## 참고

- Redis, MongoDB, MariaDB는 Compose 내부 네트워크에서만 사용합니다.
- Prometheus는 `backend`, `ai`, `prometheus`를 scrape합니다.
- Grafana datasource와 3개 대시보드는 `monitoring/grafana` 아래 파일로 자동 provision됩니다.
- GPU 사용률은 AI 컨테이너에서 `nvidia-smi`가 가능하면 실제 값을 표시하고, 사용할 수 없으면 `0`으로 표시됩니다.
