# SkinAI Frontend

SkinAI 프론트엔드 전용 저장소입니다. 백엔드 코드는 별도 저장소로 분리되었고, 이 저장소에는 Vue 3 + Vite 애플리케이션과 프론트 실행에 필요한 설정만 남깁니다.

## 프로젝트 구조

```text
.
├── frontend/            # Vue 3 + Vite 프론트엔드
├── docker-compose.yml   # 프론트엔드 컨테이너 실행 설정
└── Readme.md
```

## 사전 준비

- Node.js 22 이상
- pnpm 10.24.0 이상 또는 Corepack
- Docker로 실행할 경우 Docker와 Docker Compose

## 로컬 실행

```bash
cd frontend
corepack enable
corepack pnpm install
corepack pnpm dev
```

개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.

## Docker Compose 실행

```bash
docker compose up --build
```

백엔드 API를 프록시해야 하는 경우 실행 전에 `VITE_API_PROXY_TARGET`을 지정합니다.

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8080"
docker compose up --build
```

## 검증 명령

```bash
cd frontend
corepack pnpm lint
corepack pnpm build
```

## CI

이 저장소는 프론트엔드만 검증합니다.

- GitHub Actions: `.github/workflows/frontend-ci.yml`
- Jenkins: `Jenkinsfile`

두 파이프라인 모두 `frontend` 디렉터리에서 의존성 설치, 타입 검사, 빌드를 실행합니다. AWS 배포 단계는 추후 배포 대상이 정해진 뒤 추가합니다.

Jenkins Discord 알림을 사용하려면 Jenkins Credentials에 Discord Webhook URL을 Secret text로 등록하고 ID를 `discord-webhook-url`로 지정합니다. 빌드가 끝나면 성공/실패 상태와 빌드 URL이 Discord 채널로 전송됩니다.

## 참고

- API 연동이 필요한 프론트 계약은 `frontend/apiList.md`에 정리되어 있습니다.
- 프론트 상세 기능과 화면 구성은 `frontend/README.md`를 참고합니다.
