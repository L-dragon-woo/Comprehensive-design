# SkinAI Vue

AI 피부 시술 추천 흐름을 구현한 Vue 3 프론트엔드입니다. 사용자가 피부 사진을 촬영하거나 업로드하면 분석 진행 화면, 추천 결과, 상담, 병원 찾기, 제출 내역, 분석 기록 화면으로 이어지는 모바일 중심 UI를 제공합니다.

## 실행

```bash
corepack pnpm install
corepack pnpm dev
```

개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다.

## 명령어

```bash
corepack pnpm dev      # 개발 서버
corepack pnpm lint     # TypeScript / Vue 타입 검사
corepack pnpm build    # 타입 검사와 프로덕션 빌드
corepack pnpm preview  # 빌드 결과 미리보기
```

## 기술 스택

- Vue 3
- Vite
- TypeScript
- Vue Router
- Tailwind CSS v4
- lucide-vue-next

## 주요 화면

| 경로 | 화면 | 설명 |
| --- | --- | --- |
| `/` | 홈 | 시술 추천 시작, 최근 추천 리포트, 병원 신청 현황 |
| `/capture` | 촬영/업로드 | 사진 촬영 또는 이미지 업로드 후 분석 요청 |
| `/loading` | 분석 진행 | 추천 결과 생성 과정을 단계별 진행률로 표시 |
| `/result` | 추천 결과 | 피부 지표, 추천 시술, 상담 전 체크리스트 표시 |
| `/chat` | AI 상담 | 추천 시술 관련 질문과 답변 흐름 제공 |
| `/hospitals` | 병원 찾기 | 주변 병원 검색과 결과지 제출 흐름 제공 |
| `/history` | 분석 기록 | 날짜별 분석 기록과 점수 변화 표시 |

## 프로젝트 구조

```text
src/
  components/   공통 UI 컴포넌트
  lib/          API, 분석 기록, 병원 신청 저장 유틸
  views/        라우트별 화면 컴포넌트
  App.vue       루트 컴포넌트
  main.ts       Vue 진입점
  router.ts     라우터 설정
  styles.css    Tailwind CSS v4 테마와 전역 스타일

public/         정적 이미지와 아이콘
readme-screens/ README 화면 미리보기 이미지
```

## 백엔드 연동 메모

- 백엔드 코드는 이 저장소에서 분리되었습니다.
- 개발 서버는 `/api` 요청을 `VITE_API_PROXY_TARGET`으로 프록시합니다. 기본값은 `http://localhost:8080`입니다.
- API 계약과 연동 후보는 `apiList.md`에 정리되어 있습니다.
- 일부 화면은 현재 API 호출을 사용하므로 백엔드가 없으면 해당 요청은 실패할 수 있습니다.
