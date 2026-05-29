# SkinAI 종합 설계 프로젝트

피부 이미지 분석을 기반으로 시술 추천, AI 상담, 병원 검색 흐름을 제공하는 웹 애플리케이션입니다.  
프로젝트는 `frontend`, `backend`, `AI`, `redis` 서비스로 구성되어 있으며, 가장 간단한 실행 방법은 Docker Compose를 사용하는 것입니다.

## 프로젝트 구조

```text
.
├── frontend/            # Vue 3 + Vite 프론트엔드
├── backend/             # Spring Boot 백엔드 API
├── AI/                  # FastAPI 기반 AI 분석/상담 서버
├── docker-compose.yml   # 전체 서비스 실행 설정
└── Readme.md
```

## 사전 준비

Docker로 실행할 경우:

- Docker
- Docker Compose

개별 로컬 실행을 할 경우:

- Node.js 22 이상
- pnpm 10.24.0 이상 또는 Corepack
- Java 17
- Python 3.11
- Redis

## 전체 실행 방법

루트 디렉터리에서 다음 명령어를 실행합니다.

```bash
docker compose up --build
```

실행 후 접속 주소는 다음과 같습니다.

| 서비스 | 주소 | 설명 |
| --- | --- | --- |
| Frontend | http://localhost:5173 | 사용자 화면 |
| Backend | http://localhost:8080 | Spring Boot API |
| AI Server | http://localhost:8000 | FastAPI AI 서비스 |
| Redis | localhost:6379 | 인증 토큰 저장소 |

AI 서버 상태 확인:

```bash
curl http://localhost:8000/health
```

백엔드를 통한 AI 서버 상태 확인:

```bash
curl http://localhost:8080/api/ai/health
```

## 기본 로그인 정보

별도 환경변수를 설정하지 않으면 다음 계정으로 로그인할 수 있습니다.

```text
아이디: admin
비밀번호: admin1234
```

운영 또는 배포 환경에서는 반드시 환경변수로 변경해야 합니다.

## 환경변수

환경변수 파일은 선택 사항입니다. 필요한 경우 아래 위치에 생성합니다.

```text
backend/.env
AI/.env
```

### backend/.env 예시

```env
JWT_SECRET=change-this-secret-to-a-long-random-value
JWT_ISSUER=skinai
JWT_ACCESS_TTL_SECONDS=1800
JWT_REFRESH_TTL_SECONDS=1209600
AUTH_ADMIN_USERNAME=admin
AUTH_ADMIN_PASSWORD=admin1234
KAKAO_REST_API_KEY=
```

주요 값:

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `AI_SERVICE_URL` | AI 서버 주소 | `http://localhost:8000` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `JWT_SECRET` | JWT 서명 키 | `change-this-secret-to-a-long-random-value` |
| `AUTH_ADMIN_USERNAME` | 관리자 아이디 | `admin` |
| `AUTH_ADMIN_PASSWORD` | 관리자 비밀번호 | `admin1234` |
| `KAKAO_REST_API_KEY` | 카카오 장소 검색 REST API 키 | 없음 |

Docker Compose로 실행하면 `AI_SERVICE_URL`, Redis 호스트/포트는 compose 설정에서 자동으로 지정됩니다.

### AI/.env 예시

```env
OPENAI_API_KEY=
AI_MODEL=openai:gpt-4o-mini
```

`OPENAI_API_KEY`가 없으면 AI 상담은 내장 fallback 응답으로 동작합니다.

## 개별 로컬 실행 방법

Docker Compose 대신 각 서비스를 직접 실행할 수도 있습니다.

### 1. Redis 실행

로컬 Redis가 필요합니다. Docker로 Redis만 실행하려면 다음 명령어를 사용할 수 있습니다.

```bash
docker run --name skinai-redis -p 6379:6379 redis:7-alpine
```

### 2. AI 서버 실행

```bash
cd AI
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn api:app --host 0.0.0.0 --port 8000
```

macOS 또는 Linux에서는 가상환경 활성화 명령이 다릅니다.

```bash
source .venv/bin/activate
```

### 3. 백엔드 실행

```bash
cd backend
.\gradlew.bat bootRun
```

macOS 또는 Linux:

```bash
./gradlew bootRun
```

백엔드는 기본적으로 다음 주소의 AI 서버와 Redis에 연결합니다.

```text
AI 서버: http://localhost:8000
Redis: localhost:6379
```

### 4. 프론트엔드 실행

```bash
cd frontend
corepack enable
corepack pnpm install
corepack pnpm dev
```

프론트엔드 개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.  
`/api` 요청은 Vite proxy를 통해 `http://localhost:8080` 백엔드로 전달됩니다.

## 주요 API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 관리자 로그인 |
| `POST` | `/api/auth/refresh` | 토큰 재발급 |
| `POST` | `/api/auth/logout` | 로그아웃 |
| `GET` | `/api/ai/health` | AI 서버 상태 확인 |
| `POST` | `/api/analyses` | 이미지 분석 요청 |
| `POST` | `/api/consultations/messages` | AI 상담 메시지 전송 |
| `GET` | `/api/hospitals/search` | 카카오 병원 검색 |

## 빌드 및 점검 명령어

프론트엔드:

```bash
cd frontend
corepack pnpm lint
corepack pnpm build
```

백엔드:

```bash
cd backend
.\gradlew.bat test
.\gradlew.bat bootJar
```

AI 서버:

```bash
cd AI
python -m compileall .
```

## 종료 방법

Docker Compose로 실행한 경우:

```bash
docker compose down
```

볼륨까지 삭제하려면 다음 명령어를 사용합니다.

```bash
docker compose down -v
```

## 참고 사항

- 병원 검색 기능을 사용하려면 `KAKAO_REST_API_KEY`가 필요합니다.
- AI 이미지 분석은 `AI/pipeline/inference_models`의 모델 파일을 사용합니다.
- AI 분석 중 오류가 발생하면 mock/fallback 분석 결과가 반환되도록 구현되어 있습니다.
- 프론트엔드는 Vite 개발 서버로 실행되며, Docker 실행 시에도 `5173` 포트를 사용합니다.

## Monitoring

Docker Compose also starts Prometheus and Grafana.

| Service | URL | Notes |
| --- | --- | --- |
| Prometheus | http://localhost:9090 | Scrapes `backend`, `ai`, and Prometheus itself. |
| Grafana | http://localhost:3000 | Login with `admin` / `admin`; Prometheus is provisioned as the default datasource. |

Useful checks:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8000/metrics
```

In Grafana, try queries such as `up`, `jvm_memory_used_bytes`, or `ai_http_requests_total`.
