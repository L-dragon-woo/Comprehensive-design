# SkinAI Frontend/Backend

피부 이미지 분석을 기반으로 시술 추천, AI 상담, 병원 검색 흐름을 제공하는 웹 애플리케이션입니다.

이 레포는 프론트엔드와 백엔드만 관리합니다. AI 서비스는 별도 레포에서 관리하고, 백엔드는 `AI_SERVICE_URL` 환경변수로 AI 서버를 호출합니다.

## 프로젝트 구조

```text
.
├── frontend/            # Vue 3 + Vite 프론트엔드
├── backend/             # Spring Boot 백엔드 API
├── k8s/                 # Kubernetes 매니페스트
├── docker-compose.yml   # 프론트/백/DB/Redis 로컬 실행 설정
└── Readme.md
```

## 사전 준비

Docker로 실행할 경우:

- Docker
- Docker Compose

개별 로컬 실행의 경우:

- Node.js 22 이상
- pnpm 10.24.0 이상 또는 Corepack
- Java 17
- Redis
- MariaDB
- MongoDB
- 별도 AI 서버

## Docker Compose 실행

AI 서버를 별도 레포에서 먼저 실행한 뒤 이 레포 루트에서 실행합니다.

```bash
docker compose up --build
```

기본 접속 주소:

| 서비스 | 주소 | 설명 |
| --- | --- | --- |
| Frontend | http://localhost:5173 | 사용자 화면 |
| Backend | http://localhost:8080 | Spring Boot API |
| Redis | localhost:6379 | 인증 토큰 저장소 |
| MariaDB | localhost:3306 | 인증/서비스 DB |
| MongoDB | localhost:27017 | 분석/상담 데이터 저장소 |

백엔드를 통한 AI 서버 상태 확인:

```bash
curl http://localhost:8080/api/ai/health
```

Docker Compose에서 백엔드는 기본적으로 `http://host.docker.internal:8000`의 AI 서버를 호출합니다. 다른 주소를 쓰려면 실행 전에 `AI_SERVICE_URL`을 지정합니다.

```bash
$env:AI_SERVICE_URL="http://localhost:8000"
docker compose up --build
```

## 환경변수

필요하면 아래 파일을 생성합니다.

```text
backend/.env
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
AI_SERVICE_URL=http://localhost:8000
```

주요 값:

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `AI_SERVICE_URL` | 별도 AI 서버 주소 | `http://localhost:8000` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `SPRING_DATASOURCE_URL` | MariaDB JDBC URL | `jdbc:mariadb://localhost:3306/skinai_auth` |
| `SPRING_DATA_MONGODB_URI` | MongoDB URL | `mongodb://localhost:27017/skinai` |
| `JWT_SECRET` | JWT 서명 키 | `change-this-secret-to-a-long-random-value` |
| `AUTH_ADMIN_USERNAME` | 관리자 아이디 | `admin` |
| `AUTH_ADMIN_PASSWORD` | 관리자 비밀번호 | `admin1234` |
| `KAKAO_REST_API_KEY` | 카카오 장소 검색 REST API 키 | 없음 |

## 개별 로컬 실행

### 1. Redis 실행

```bash
docker run --name skinai-redis -p 6379:6379 redis:7-alpine
```

### 2. MariaDB 실행

```bash
docker run --name skinai-mariadb -p 3306:3306 -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=skinai_auth -e MARIADB_USER=skinai -e MARIADB_PASSWORD=skinai mariadb:11
```

### 3. MongoDB 실행

```bash
docker run --name skinai-mongo -p 27017:27017 mongo:7
```

### 4. AI 서버 실행

AI 서버는 별도 레포에서 실행합니다. 백엔드에는 해당 주소를 `AI_SERVICE_URL`로 전달합니다.

```env
AI_SERVICE_URL=http://localhost:8000
```

### 5. 백엔드 실행

```bash
cd backend
.\gradlew.bat bootRun
```

macOS 또는 Linux:

```bash
cd backend
./gradlew bootRun
```

### 6. 프론트엔드 실행

```bash
cd frontend
corepack enable
corepack pnpm install
corepack pnpm dev
```

프론트엔드 개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.

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

## 빌드 및 검증

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

## 종료

Docker Compose로 실행한 경우:

```bash
docker compose down
```

볼륨까지 삭제하려면:

```bash
docker compose down -v
```

## 참고

- AI 서비스 코드는 이 레포에 포함하지 않습니다.
- 배포 환경에서는 `AI_SERVICE_URL`을 실제 AI 서비스 주소로 설정해야 합니다.
- 병원 검색 기능에는 `KAKAO_REST_API_KEY`가 필요합니다.
