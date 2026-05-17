# SkinAI 필요 API 정리

현재 프론트엔드는 백엔드 없이 정적 데이터와 `localStorage`로 동작합니다. 실제 서비스 연동 시 필요한 API를 `AI 쪽과 소통이 필요한 API`와 `일반 백엔드만으로 가능한 API`로 분류했습니다.

## 분류 기준

- AI 연동 필요: 이미지 분석, 피부 지표 산출, 시술 추천 생성, 자연어 상담처럼 AI 모델 또는 AI 서버의 결과가 필요한 API
- AI 연동 불필요: 병원 데이터 조회, 결과지 제출 저장, 신청 현황 조회, 기존 분석 기록 조회처럼 DB/파일/비즈니스 로직만으로 처리 가능한 API
- 부분 AI 연동: API 자체는 일반 백엔드가 제공하지만, 응답 데이터가 사전에 AI 분석 결과를 필요로 하는 경우

## 우선 구현 전략

AI 서버가 아직 준비되지 않았다면 아래 순서가 현실적입니다.

1. 일반 백엔드 API부터 구현
2. 분석 결과는 임시 mock 데이터 또는 DB seed 데이터로 제공
3. AI 연동 API는 같은 endpoint 계약만 먼저 고정
4. AI 서버 준비 후 내부 구현만 mock에서 AI 호출로 교체

## 전체 API 분류 요약

| 분류 | Method | Endpoint | 용도 | AI 필요 여부 |
| --- | --- | --- | --- | --- |
| 분석 | `POST` | `/api/analyses` | 촬영/업로드 이미지로 새 분석 생성 | 필요 |
| 분석 | `GET` | `/api/analyses/{analysisId}/status` | 분석 진행 상태 조회 | 부분 필요 |
| 분석 | `GET` | `/api/analyses/{analysisId}` | 분석 결과 상세 조회 | 부분 필요 |
| 기록 | `GET` | `/api/analyses` | 분석 기록 목록 조회 | 불필요 |
| 상담 | `POST` | `/api/consultations/messages` | AI 시술 상담 메시지 전송 | 필요 |
| 병원 | `GET` | `/api/hospitals` | 추천 병원 검색/목록 조회 | 불필요 |
| 병원 | `GET` | `/api/hospitals/{hospitalId}` | 병원 상세 조회 | 불필요 |
| 제출 | `POST` | `/api/hospital-applications` | 병원에 분석 결과지 제출 | 불필요 |
| 제출 | `GET` | `/api/hospital-applications` | 병원 제출/신청 현황 조회 | 불필요 |
| 제출 | `GET` | `/api/hospital-applications/{applicationId}` | 병원 제출 상세 조회 | 불필요 |

## AI 쪽과 소통이 필요한 API

### 1. 새 분석 생성

이미지를 받아 AI 분석 작업을 생성합니다. 실제 AI 서버가 준비되기 전에는 이미지만 저장하고 mock 분석 결과를 생성해도 됩니다.

```http
POST /api/analyses
Content-Type: multipart/form-data
```

#### Request

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `image` | `File` | Y | 촬영 또는 업로드한 피부 이미지 |
| `targetArea` | `string` | N | 분석 부위. 예: `face`, `cheek`, `forehead` |
| `memo` | `string` | N | 사용자가 입력한 피부 고민 |

#### Response

```json
{
  "analysisId": "analysis_001",
  "status": "processing",
  "createdAt": "2026-05-14T09:00:00.000Z"
}
```

#### AI 준비 전 처리 방식

- 업로드 이미지를 저장합니다.
- `analysisId`를 생성합니다.
- status를 `processing` 또는 바로 `completed`로 저장합니다.
- 분석 결과는 고정 mock 데이터로 생성합니다.

### 2. AI 시술 상담 메시지 전송

사용자의 질문과 분석 결과를 기반으로 AI 답변을 생성합니다.

```http
POST /api/consultations/messages
Content-Type: application/json
```

#### Request

```json
{
  "analysisId": "analysis_001",
  "message": "리쥬란과 피코토닝 중 뭐가 더 우선이야?",
  "history": [
    {
      "role": "assistant",
      "content": "안녕하세요! SkinAI 시술 상담사예요."
    }
  ]
}
```

#### Response

```json
{
  "messageId": "msg_001",
  "role": "assistant",
  "content": "현재 결과만 보면 리쥬란 힐러 상담을 먼저 받아보는 흐름이 자연스러워요.",
  "createdAt": "2026-05-14T09:05:00.000Z"
}
```

#### AI 준비 전 처리 방식

- 프론트의 현재 `getAIResponse()`처럼 키워드 기반 응답을 백엔드에서 임시 제공할 수 있습니다.
- 또는 FAQ/추천 시술 설명 템플릿 기반으로 응답합니다.

## 부분적으로 AI 결과가 필요한 API

이 API들은 호출 시점에 AI 서버와 직접 통신하지 않아도 됩니다. 다만 응답 데이터는 과거에 AI 분석 또는 mock 분석을 통해 생성되어 있어야 합니다.

### 3. 분석 진행 상태 조회

분석 진행 화면에서 진행률과 현재 단계를 보여줄 때 사용합니다.

```http
GET /api/analyses/{analysisId}/status
```

#### Response

```json
{
  "analysisId": "analysis_001",
  "status": "processing",
  "progress": 60,
  "currentStep": "pores_texture",
  "steps": [
    { "key": "detect_skin_area", "label": "피부 영역 감지", "status": "completed" },
    { "key": "skin_tone", "label": "피부 톤 분석", "status": "completed" },
    { "key": "pores_texture", "label": "모공 및 결 분석", "status": "processing" },
    { "key": "consultation_points", "label": "시술 상담 포인트 정리", "status": "pending" },
    { "key": "treatment_recommendation", "label": "맞춤 시술 추천 생성", "status": "pending" }
  ]
}
```

#### AI 준비 전 처리 방식

- 타이머 또는 DB 상태값으로 진행률을 흉내냅니다.
- 일정 시간이 지나면 `completed`로 변경하고 mock 결과를 연결합니다.

### 4. 분석 결과 상세 조회

결과 화면과 기록 상세 화면에서 사용합니다.

```http
GET /api/analyses/{analysisId}
```

#### Response

```json
{
  "id": "analysis_001",
  "date": "2026-04-30",
  "dateFormatted": "2026.04.30",
  "overallScore": 78,
  "skinType": "복합성",
  "concerns": ["T존 유분 과다", "볼 색소침착", "수분 부족"],
  "metrics": [
    {
      "id": "hydration",
      "title": "수분",
      "score": 72,
      "status": "보통",
      "description": "피부 수분이 약간 부족해요"
    },
    {
      "id": "sebum",
      "title": "유분",
      "score": 65,
      "status": "주의",
      "description": "T존 유분이 과다해요"
    }
  ],
  "treatments": [
    {
      "id": "treatment_001",
      "name": "리쥬란 힐러",
      "match": "추천",
      "reason": "볼 건조와 피부결 개선 상담에 적합해요",
      "note": "민감도와 통증 정도를 상담하세요"
    }
  ],
  "recommendations": [
    "시술 전 1주일은 강한 각질 케어 피하기",
    "상담 시 색소침착 부위와 민감도 공유하기",
    "시술 후 자외선 차단과 보습 계획 세우기"
  ],
  "imageUrl": "https://example.com/images/analysis_001.jpg"
}
```

#### AI 준비 전 처리 방식

- `src/views/ResultView.vue`의 `skinAnalysisData`와 동일한 mock 결과를 DB에 저장하거나 API 응답으로 반환합니다.
- 프론트는 실제 AI 여부와 관계없이 같은 응답 구조를 사용하면 됩니다.

## AI 쪽과 소통이 필요 없는 API

### 5. 분석 기록 목록 조회

이미 저장된 분석 결과 목록을 조회합니다. AI 서버 호출은 필요 없습니다.

```http
GET /api/analyses?period=all&page=1&pageSize=20
```

#### Query

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `period` | `all \| month \| week` | N | 기간 필터 |
| `page` | `number` | N | 페이지 번호 |
| `pageSize` | `number` | N | 페이지 크기 |

#### Response

```json
{
  "items": [
    {
      "id": "analysis_001",
      "date": "2026-04-30",
      "dateFormatted": "2026.04.30",
      "score": 82,
      "change": 5,
      "improvements": ["수분 개선", "모공 케어"]
    }
  ],
  "total": 1
}
```

### 6. 병원 검색/목록 조회

병원 DB와 추천 시술명 매칭만으로 처리할 수 있습니다. AI 서버 호출은 필요 없습니다.

```http
GET /api/hospitals?query=피코토닝&lat=37.4979&lng=127.0276&treatments=피코토닝,리쥬란%20힐러
```

#### Query

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `query` | `string` | N | 병원명, 주소, 시술명 검색어 |
| `lat` | `number` | N | 현재 위치 위도 |
| `lng` | `number` | N | 현재 위치 경도 |
| `treatments` | `string` | N | 추천 시술명 목록. 쉼표 구분 |
| `sort` | `distance \| rating` | N | 정렬 기준 |

#### Response

```json
{
  "items": [
    {
      "id": "hospital_001",
      "name": "서울스킨 피부과의원",
      "distance": "0.8km",
      "distanceMeters": 800,
      "rating": 4.8,
      "address": "서울 강남구 테헤란로 142",
      "specialties": ["리쥬란", "피코토닝", "스킨부스터"],
      "matchedTreatments": ["리쥬란 힐러", "피코토닝"],
      "waitTime": "오늘 상담 가능",
      "phone": "02-1234-5678"
    }
  ],
  "total": 1
}
```

### 7. 병원 상세 조회

```http
GET /api/hospitals/{hospitalId}
```

#### Response

```json
{
  "id": "hospital_001",
  "name": "서울스킨 피부과의원",
  "rating": 4.8,
  "address": "서울 강남구 테헤란로 142",
  "phone": "02-1234-5678",
  "specialties": ["리쥬란", "피코토닝", "스킨부스터"],
  "treatments": [
    {
      "name": "리쥬란 힐러",
      "description": "피부결과 건조 고민 상담에 적합"
    }
  ],
  "availableTimes": ["오늘 상담 가능", "내일 오전 가능"]
}
```

### 8. 병원 분석 결과지 제출

선택한 병원에 어떤 분석 결과와 항목을 제출했는지 저장합니다. AI 서버 호출은 필요 없습니다.

```http
POST /api/hospital-applications
Content-Type: application/json
```

#### Request

```json
{
  "analysisId": "analysis_001",
  "hospitalId": "hospital_001",
  "includedItems": ["추천 시술 목록", "피부 점수와 지표", "촬영 이미지"],
  "consent": true
}
```

#### Response

```json
{
  "id": "application_001",
  "analysisId": "analysis_001",
  "hospitalId": "hospital_001",
  "hospitalName": "서울스킨 피부과의원",
  "submittedAt": "2026-05-14T09:10:00.000Z",
  "status": "submitted",
  "includedItems": ["추천 시술 목록", "피부 점수와 지표", "촬영 이미지"]
}
```

### 9. 병원 신청 현황 조회

홈 화면의 `병원 신청 현황` 카드에서 사용합니다.

```http
GET /api/hospital-applications?latest=true
```

#### Response

```json
{
  "items": [
    {
      "id": "application_001",
      "hospitalName": "서울스킨 피부과의원",
      "submittedAt": "2026-05-14T09:10:00.000Z",
      "status": "submitted",
      "includedItems": ["추천 시술 목록", "피부 점수와 지표", "촬영 이미지"]
    }
  ],
  "total": 1
}
```

### 10. 병원 신청 상세 조회

```http
GET /api/hospital-applications/{applicationId}
```

#### Response

```json
{
  "id": "application_001",
  "analysisId": "analysis_001",
  "hospital": {
    "id": "hospital_001",
    "name": "서울스킨 피부과의원",
    "phone": "02-1234-5678",
    "address": "서울 강남구 테헤란로 142"
  },
  "submittedAt": "2026-05-14T09:10:00.000Z",
  "status": "submitted",
  "includedItems": ["추천 시술 목록", "피부 점수와 지표", "촬영 이미지"]
}
```

## 상태값

### 분석 상태

| 값 | 설명 |
| --- | --- |
| `processing` | 분석 진행 중 |
| `completed` | 분석 완료 |
| `failed` | 분석 실패 |

### 병원 신청 상태

| 값 | 설명 |
| --- | --- |
| `submitted` | 제출 완료 |
| `reviewing` | 병원 검토 중 |
| `confirmed` | 예약 확정 |
| `rejected` | 제출 반려 또는 상담 불가 |

## AI 준비 전 우선 구현 순서

AI가 아직 준비되지 않은 상태에서는 다음 API부터 구현하면 프론트 기능 대부분을 연결할 수 있습니다.

1. `GET /api/hospitals`
2. `GET /api/hospitals/{hospitalId}`
3. `POST /api/hospital-applications`
4. `GET /api/hospital-applications?latest=true`
5. `GET /api/hospital-applications/{applicationId}`
6. `GET /api/analyses`
7. `GET /api/analyses/{analysisId}` mock 응답
8. `GET /api/analyses/{analysisId}/status` mock 진행률
9. `POST /api/analyses` mock 분석 생성
10. `POST /api/consultations/messages` mock 상담 응답

## 프론트엔드 교체 대상

| 현재 위치 | 현재 방식 | API 연동 시 교체 | AI 필요 여부 |
| --- | --- | --- | --- |
| `src/views/CaptureView.vue` | `/placeholder.jpg`와 로컬 미리보기 | `POST /api/analyses` | 필요 |
| `src/views/LoadingView.vue` | 타이머 기반 진행률 | `GET /api/analyses/{analysisId}/status` | 부분 필요 |
| `src/views/ResultView.vue` | 정적 `skinAnalysisData` | `GET /api/analyses/{analysisId}` | 부분 필요 |
| `src/views/ChatView.vue` | 키워드 조건 분기 응답 | `POST /api/consultations/messages` | 필요 |
| `src/views/HospitalView.vue` | 정적 `hospitals` 배열 | `GET /api/hospitals` | 불필요 |
| `src/lib/skinai.ts` | `localStorage` 신청 저장 | `POST/GET /api/hospital-applications` | 불필요 |
| `src/views/HistoryView.vue` | 정적 `historyData` | `GET /api/analyses` | 불필요 |
