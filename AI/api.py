from __future__ import annotations

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


class ChatResponse(BaseModel):
    sessionId: str
    content: str


def _fallback_chat(message: str) -> str:
    lowered = message.lower()
    if "priority" in lowered or "먼저" in message or "우선" in message or "순위" in message:
        return "현재 결과 기준으로는 수분과 탄력 보완 상담을 먼저 받고, 색소 고민이 크다면 피코토닝 계열 상담을 함께 비교해보는 흐름이 좋습니다."
    if "주의" in message or "확인" in message:
        return "시술 전에는 최근 피부 시술 이력, 알레르기, 복용 중인 약, 임신 가능성, 회복 가능 기간을 상담실에 꼭 공유해 주세요."
    return "분석 결과를 기준으로 피부 장벽, 수분, 색소 고민을 함께 보면서 시술 강도와 회복 기간을 조절하는 상담을 추천드립니다."


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai"}


@app.post("/api/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    session_id = request.sessionId or str(uuid.uuid4())
    try:
        from agent.graph import ChatSession

        session = ChatSession(thread_id=session_id)
        content = session.send(request.message)
        if not content.strip():
            content = _fallback_chat(request.message)
    except Exception:
        content = _fallback_chat(request.message)
    return ChatResponse(sessionId=session_id, content=content)


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
            return {"status": status, "imagePath": str(image_path), "result": result}
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
