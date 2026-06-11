# EC2 발표 배포 가이드

## 구성

```text
dgu.skinaiteam10.com
  -> EC2 Elastic IP
  -> Caddy HTTPS
       -> /api/* : backend
       -> /*     : frontend

backend -> AI, MariaDB, MongoDB, Redis
backend -> private S3 bucket for images and reports
```

발표용 구성은 EC2 한 대에서 Docker Compose 전체 서비스를 실행합니다. 촬영 이미지와 보고서는 EC2 디스크가 아니라 비공개 S3 버킷에 저장합니다.

현재 보고서 저장 API는 실제 PDF 바이너리가 아니라 브라우저에서 인쇄 가능한 HTML 파일을 `reports/` 경로에 저장합니다. 사용자는 브라우저 인쇄 화면에서 PDF로 저장할 수 있습니다. 실제 `.pdf` 파일 자체를 S3에 보관하려면 별도의 PDF 생성 기능이 필요합니다.

## AWS 준비

- EC2: Ubuntu 24.04, x86_64, `t3.xlarge`, gp3 60GB
- Security Group 인바운드: TCP 80, TCP 443
- Route53 `dgu.skinaiteam10.com` A 레코드: EC2 Elastic IP
- EC2 IAM Role:
  - `s3:ListBucket`
  - `s3:GetObject`
  - `s3:PutObject`
  - `s3:DeleteObject`
- S3 버킷은 Block Public Access를 유지합니다.

브라우저에서 presigned 이미지 업로드를 사용할 경우 S3 CORS를 설정합니다.

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedOrigins": ["https://dgu.skinaiteam10.com"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

## 서버 설치

EC2 Session Manager 또는 SSH에서 실행합니다.

```bash
sudo apt-get update
sudo apt-get install -y git docker.io docker-compose-v2
sudo usermod -aG docker ubuntu
newgrp docker

git clone https://github.com/L-dragon-woo/Comprehensive-design.git
cd Comprehensive-design
git checkout dev
```

## 환경변수

저장소 루트 `.env`:

```env
ACME_EMAIL=your-email@example.com
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=change-this-password
VITE_KAKAO_MAP_APP_KEY=
AI_UVICORN_WORKERS=1
```

`backend/.env`:

```env
JWT_SECRET=replace-with-a-long-random-value
AUTH_ADMIN_USERNAME=admin
AUTH_ADMIN_PASSWORD=replace-this-password
KAKAO_REST_API_KEY=
AWS_REGION=ap-northeast-2
S3_BUCKET=replace-with-private-bucket-name
S3_PRESIGNED_URL_TTL=PT1H
```

`AI/.env`에는 실제 OpenAI 및 Neo4j 값을 입력합니다. `.env` 파일들은 Git에 커밋하지 않습니다.

Kakao Developers Web 플랫폼 허용 도메인에 다음 주소를 등록합니다.

```text
https://dgu.skinaiteam10.com
```

## 실행

```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.demo.yml ps
```

Caddy가 Route53 DNS를 확인한 뒤 Let's Encrypt 인증서를 자동 발급합니다.

## 확인

```bash
curl -I https://dgu.skinaiteam10.com
curl https://dgu.skinaiteam10.com/api/ai/health
docker compose -f docker-compose.yml -f docker-compose.demo.yml exec ai python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/ready').read().decode())"
docker compose -f docker-compose.yml -f docker-compose.demo.yml logs --tail=100 caddy backend ai
```

발표 전에 로그인, 촬영, 분석, 병원 제출, S3 이미지 및 보고서 저장을 직접 확인합니다. AI 모델은 worker마다 중복 로딩되므로 발표 환경에서는 `AI_UVICORN_WORKERS=1`을 유지합니다.

## 종료

발표 종료 후:

```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml down
```

필요한 S3 파일을 확인한 뒤 EC2를 종료하고 Elastic IP를 해제합니다. EC2를 중지하기만 하면 EBS와 Elastic IP 비용은 계속 발생합니다.
