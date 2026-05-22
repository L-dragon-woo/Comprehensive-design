from __future__ import annotations

import json
import shutil
import sys
import uuid
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

ROOT = Path(__file__).resolve().parent
BEAUTY_AGENT = ROOT / "beauty-agent"
PIPELINE_DIR = ROOT / "pipeline"
UPLOAD_DIR = ROOT / "uploads"
LAST_ANALYSIS_PATH = ROOT / "analysis-result.json"

for path in (BEAUTY_AGENT, PIPELINE_DIR):
    if str(path) not in sys.path:
        sys.path.insert(0, str(path))

load_dotenv(ROOT / ".env")

app = FastAPI(title="SkinAI AI Service")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    message: str
    sessionId: str | None = None
    analysis: dict[str, Any] | None = None


class ChatResponse(BaseModel):
    sessionId: str
    content: str
    mode: str = "fallback"


def _load_latest_analysis() -> dict[str, Any] | None:
    if not LAST_ANALYSIS_PATH.exists():
        return None
    try:
        with LAST_ANALYSIS_PATH.open(encoding="utf-8") as file:
            data = json.load(file)
        return data.get("result", data) if isinstance(data, dict) else None
    except Exception:
        return None


def _score_line(label: str, value: Any) -> str:
    if isinstance(value, (int, float)):
        return f"{label} {value:.1f}점"
    return f"{label} 확인 필요"


def _analysis_summary(analysis: dict[str, Any] | None) -> str:
    if not analysis:
        return "현재 저장된 분석 결과가 없어 일반적인 상담 기준으로 안내드릴게요."

    age = analysis.get("age")
    pigment = analysis.get("pigment") or {}
    wrinkle = analysis.get("wrinkle") or {}
    homogenity = analysis.get("homogenity") or {}

    parts: list[str] = []
    if isinstance(age, (int, float)):
        parts.append(f"예상 피부 나이는 {age:.1f}세")
    if pigment:
        parts.append(
            "색소는 "
            + ", ".join(
                [
                    _score_line("좌측", pigment.get("left")),
                    _score_line("우측", pigment.get("right")),
                ]
            )
        )
    if wrinkle:
        strongest = sorted(
            [(key, value) for key, value in wrinkle.items() if isinstance(value, (int, float))],
            key=lambda item: item[1],
        )[:2]
        if strongest:
            parts.append("상대적으로 관리가 필요한 주름 부위는 " + ", ".join(f"{key} {value:.1f}점" for key, value in strongest))
    if homogenity:
        parts.append(
            "피부 균일도는 "
            + ", ".join(
                [
                    _score_line("광채", homogenity.get("radiance")),
                    _score_line("결", homogenity.get("texture")),
                ]
            )
        )
    return " / ".join(parts) if parts else "분석 결과는 있지만 세부 점수를 읽지 못했습니다."


def _fallback_chat(message: str, analysis: dict[str, Any] | None = None) -> str:
    normalized = message.lower()
    summary = _analysis_summary(analysis)

    if any(keyword in message for keyword in ["나이", "몇 살", "연령"]) or "age" in normalized:
        return f"{summary}\n\n나이 추정은 진단값이 아니라 이미지 기반 모델 추정치입니다. 조명, 표정, 화질에 따라 달라질 수 있어서 참고용으로만 보는 게 좋습니다."

    if any(keyword in message for keyword in ["색소", "잡티", "기미", "피코"]) or "pigment" in normalized:
        return f"{summary}\n\n색소 고민은 피코토닝 계열 상담을 우선 고려할 수 있습니다. 다만 자외선 노출, 최근 레이저 이력, 피부 민감도에 따라 강도와 주기를 조절해야 합니다."

    if any(keyword in message for keyword in ["주름", "탄력", "리쥬란", "회복"]) or "wrinkle" in normalized:
        return f"{summary}\n\n주름과 탄력 고민은 리쥬란, 스킨부스터, 탄력 장비 상담을 비교해볼 수 있습니다. 회복 기간이 짧아야 한다면 시술 강도를 낮춰 시작하는 편이 안전합니다."

    if any(keyword in message for keyword in ["우선", "먼저", "순위", "추천"]) or "priority" in normalized:
        return f"{summary}\n\n우선순위는 1) 피부 장벽과 수분 관리, 2) 색소 상담, 3) 탄력/주름 상담 순서가 무난합니다. 한 번에 강한 시술을 묶기보다 반응을 보면서 단계적으로 진행하는 것을 추천드립니다."

    if any(keyword in message for keyword in ["주의", "부작용", "확인", "하면 안"]) or "risk" in normalized:
        return "시술 전에는 최근 시술 이력, 알레르기, 복용 중인 약, 임신 가능성, 켈로이드 체질, 햇빛 노출 계획을 꼭 상담실에 공유해 주세요. 붉어짐이나 색소침착 위험도 함께 확인하는 것이 좋습니다."

    return f"{summary}\n\n궁금한 항목을 색소, 주름, 탄력, 수분, 시술 우선순위처럼 조금 더 구체적으로 물어보면 그 기준에 맞춰 상담 방향을 정리해드릴게요."


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai"}


@app.post("/api/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    session_id = request.sessionId or str(uuid.uuid4())
    analysis = request.analysis or _load_latest_analysis()
    try:
        from agent.graph import ChatSession

        session = ChatSession(thread_id=session_id)
        content = session.send(request.message)
        if content.strip():
            return ChatResponse(sessionId=session_id, content=content, mode="llm")
    except Exception:
        pass
    return ChatResponse(sessionId=session_id, content=_fallback_chat(request.message, analysis), mode="fallback")


@app.post("/api/analyze")
async def analyze(
    image: UploadFile = File(...),
    gender: str = Form("female"),
) -> dict[str, Any]:
    if image.content_type and image.content_type not in {"image/jpeg", "image/png", "image/webp"}:
        return {
            "status": "rejected",
            "message": "Unsupported image type. Upload JPEG, PNG, or WebP.",
            "result": None,
        }

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    suffix = Path(image.filename or "image.jpg").suffix or ".jpg"
    image_path = UPLOAD_DIR / f"{uuid.uuid4()}{suffix}"
    with image_path.open("wb") as target:
        shutil.copyfileobj(image.file, target)

    try:
        from pipeline import SkinPipeline

        pipe = SkinPipeline(PIPELINE_DIR / "config.yaml", gender=gender)
        result = pipe.predict_single(str(image_path))
        if result:
            status = result.get("status", "completed") if isinstance(result, dict) else "completed"
            response = {"status": status, "imagePath": str(image_path), "result": result}
            with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
                json.dump(response, file, ensure_ascii=False, indent=2)
            return response
    except Exception as exc:
        return {
            "status": "fallback",
            "imagePath": str(image_path),
            "message": f"AI pipeline fallback: {exc}",
            "result": _mock_analysis(gender),
        }

    return {"status": "fallback", "imagePath": str(image_path), "result": _mock_analysis(gender)}


def _mock_analysis(gender: str) -> dict[str, Any]:
    return {
        "gender": gender,
        "overallScore": 72,
        "skinType": "combination",
        "topConcerns": ["hydration", "pigment", "texture"],
        "scores": {
            "hydration": 65,
            "sebum": 78,
            "pores": 70,
            "pigment": 68,
        },
        "recommendations": ["Rejuran Healer", "Pico toning", "Aquapeel"],
    }
