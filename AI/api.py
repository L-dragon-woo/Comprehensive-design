from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, Request, Response, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Gauge, Histogram, generate_latest
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

REQUEST_COUNT = Counter(
    "ai_http_requests_total",
    "Total HTTP requests handled by the AI service.",
    ["method", "path", "status"],
)
REQUEST_LATENCY = Histogram(
    "ai_http_request_duration_seconds",
    "HTTP request latency for the AI service.",
    ["method", "path"],
)
INFERENCE_COUNT = Counter(
    "ai_inference_count",
    "Total AI image inference requests.",
    ["status"],
)
INFERENCE_DURATION = Histogram(
    "ai_inference_duration_seconds",
    "AI image inference duration.",
)
GPU_UTILIZATION = Gauge(
    "ai_gpu_utilization_percent",
    "GPU utilization percentage reported by nvidia-smi. Zero when no GPU is available.",
)


@app.middleware("http")
async def collect_http_metrics(request: Request, call_next):
    if request.url.path == "/metrics":
        return await call_next(request)

    start = time.perf_counter()
    route = request.scope.get("route")
    path = getattr(route, "path", request.url.path)
    status_code = 500
    try:
        response = await call_next(request)
        status_code = response.status_code
        return response
    finally:
        REQUEST_COUNT.labels(request.method, path, str(status_code)).inc()
        REQUEST_LATENCY.labels(request.method, path).observe(time.perf_counter() - start)


class ChatRequest(BaseModel):
    message: str
    sessionId: str | None = None
    analysis: dict[str, Any] | None = None


class ChatResponse(BaseModel):
    sessionId: str
    content: str
    mode: str = "fallback"


def _t(text: str) -> str:
    """Return Korean text from ASCII-only unicode escapes."""
    return re.sub(r"\\u([0-9a-fA-F]{4})", lambda match: chr(int(match.group(1), 16)), text)


def _load_latest_analysis() -> dict[str, Any] | None:
    if not LAST_ANALYSIS_PATH.exists():
        return None
    try:
        with LAST_ANALYSIS_PATH.open(encoding="utf-8-sig") as file:
            data = json.load(file)
        return data.get("result", data) if isinstance(data, dict) else None
    except Exception:
        return None


def _score_line(label: str, value: Any) -> str:
    if isinstance(value, (int, float)):
        return f"{label} {value:.1f}\uc810"
    return f"{label} \ud655\uc778 \ud544\uc694"


def _analysis_summary(analysis: dict[str, Any] | None) -> str:
    if not analysis:
        return _t(
            "\\ud604\uc7ac \uc800\uc7a5\ub41c \ubd84\uc11d \uacb0\uacfc\uac00 \uc5c6\uc5b4 "
            "\uc77c\ubc18\uc801\uc778 \uc0c1\ub2f4 \uae30\uc900\uc73c\ub85c \uc548\ub0b4\ud560\uac8c\uc694."
        )

    age = analysis.get("age")
    pigment = analysis.get("pigment") or {}
    wrinkle = analysis.get("wrinkle") or {}
    homogenity = analysis.get("homogenity") or {}

    parts: list[str] = []
    if isinstance(age, (int, float)):
        parts.append(_t("\\uc608\uc0c1 \ud53c\ubd80 \ub098\uc774 ") + f"{age:.1f}\uc138")
    if pigment:
        parts.append(
            _t("\\uc0c9\uc18c\ub294 ")
            + ", ".join(
                [
                    _score_line(_t("\\uc88c\uce21"), pigment.get("left")),
                    _score_line(_t("\\uc6b0\uce21"), pigment.get("right")),
                ]
            )
        )
    if wrinkle:
        weakest = sorted(
            [(key, value) for key, value in wrinkle.items() if isinstance(value, (int, float))],
            key=lambda item: item[1],
        )[:2]
        if weakest:
            parts.append(
                _t("\\uc6b0\uc120 \uad00\ub9ac\uac00 \ud544\uc694\ud55c \uc8fc\ub984 \ubd80\uc704\ub294 ")
                + ", ".join(f"{key} {value:.1f}\uc810" for key, value in weakest)
            )
    if homogenity:
        parts.append(
            _t("\\ud53c\ubd80 \uade0\uc77c\ub3c4\ub294 ")
            + ", ".join(
                [
                    _score_line(_t("\\uad11\ucc44"), homogenity.get("radiance")),
                    _score_line(_t("\\uacb0"), homogenity.get("texture")),
                ]
            )
        )
    return " / ".join(parts) if parts else _t("\\ubd84\uc11d \uac12\uc744 \uc77d\uc744 \uc218 \uc5c6\uc5b4\uc694.")


def _fallback_chat(message: str, analysis: dict[str, Any] | None = None) -> str:
    normalized = message.lower()
    summary = _analysis_summary(analysis)

    if any(keyword in message for keyword in [_t("\\ub098\uc774"), _t("\\uba87 \\uc0b4"), _t("\\uc5f0\ub839")]) or "age" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\ub098\uc774 \ucd94\uc815\uc740 \uc9c4\ub2e8\uac12\uc774 \uc544\ub2c8\ub77c \uc774\ubbf8\uc9c0 "
                "\uae30\ubc18 \ubaa8\ub378 \ucd94\uc815\uce58\uc785\ub2c8\ub2e4. \uc870\uba85, \ud45c\uc815, "
                "\ud654\uc7a5\uc5d0 \ub530\ub77c \ub2ec\ub77c\uc9c8 \uc218 \uc788\uc5b4\uc11c \ucc38\uace0\uc6a9\uc73c\ub85c\ub9cc "
                "\ubcf4\ub294 \uac83\uc774 \uc88b\uc2b5\ub2c8\ub2e4."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc0c9\uc18c"), _t("\\uc7a1\ud2f0"), _t("\\uae30\ubbf8"), _t("\\ud53c\ucf54")]) or "pigment" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc0c9\uc18c \uace0\ubbfc\uc740 \ud53c\ucf54\ud1a0\ub2dd \uacc4\uc5f4 \uc0c1\ub2f4\uc744 "
                "\uc6b0\uc120 \uace0\ub824\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4. \ub2e4\ub9cc \uc790\uc678\uc120 "
                "\ub178\ucd9c, \ucd5c\uadfc \ub808\uc774\uc800 \uc774\ub825, \ud53c\ubd80 \ubbfc\uac10\ub3c4\uc5d0 "
                "\ub530\ub77c \uac15\ub3c4\uc640 \uc8fc\uae30\ub97c \uc870\uc808\ud574\uc57c \ud569\ub2c8\ub2e4."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc8fc\ub984"), _t("\\ud0c4\ub825"), _t("\\ub9ac\uc96c\ub780"), _t("\\ud68c\ubcf5")]) or "wrinkle" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc8fc\ub984\uacfc \ud0c4\ub825 \uace0\ubbfc\uc740 \ub9ac\uc96c\ub780, \uc2a4\ud0a8\ubd80\uc2a4\ud130, "
                "\ud0c4\ub825 \uc7a5\ube44 \uc0c1\ub2f4\uc744 \ube44\uad50\ud574\ubcfc \uc218 \uc788\uc2b5\ub2c8\ub2e4. "
                "\ud68c\ubcf5 \uae30\uac04\uc774 \uc9e7\uc544\uc57c \ud55c\ub2e4\uba74 \uac15\ub3c4\ub97c \ub0ae\ucdb0 "
                "\ub2e8\uacc4\uc801\uc73c\ub85c \uc9c4\ud589\ud558\ub294 \ud3b8\uc774 \uc548\uc804\ud569\ub2c8\ub2e4."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc6b0\uc120"), _t("\\uba3c\uc800"), _t("\\uc21c\uc704"), _t("\\ucd94\ucc9c")]) or "priority" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc6b0\uc120\uc21c\uc704\ub294 1) \ud53c\ubd80 \uc7a5\ubcbd\uacfc \uc218\ubd84 \uad00\ub9ac, "
                "2) \uc0c9\uc18c \uc0c1\ub2f4, 3) \ud0c4\ub825/\uc8fc\ub984 \uc0c1\ub2f4 \uc21c\uc11c\uac00 "
                "\ubb34\ub09c\ud569\ub2c8\ub2e4. \ud55c \ubc88\uc5d0 \uac15\ud55c \uc2dc\uc220\uc744 \ubb36\uae30\ubcf4\ub2e4 "
                "\ubc18\uc751\uc744 \ubcf4\uba74\uc11c \ub2e8\uacc4\uc801\uc73c\ub85c \uc9c4\ud589\ud558\ub294 \uac83\uc744 "
                "\ucd94\ucc9c\ud569\ub2c8\ub2e4."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc8fc\uc758"), _t("\\ubd80\uc791\uc6a9"), _t("\\ud655\uc778"), _t("\\ud558\uba74 \\uc548")]) or "risk" in normalized:
        return _t(
            "\\uc2dc\uc220 \uc804\uc5d0\ub294 \ucd5c\uadfc \uc2dc\uc220 \uc774\ub825, \uc54c\ub808\ub974\uae30, "
            "\ubcf5\uc6a9 \uc911\uc778 \uc57d, \uc784\uc2e0 \uac00\ub2a5\uc131, \ucf08\ub85c\uc774\ub4dc \uccb4\uc9c8, "
            "\ud0c4\ub2dd \ubc0f \uc790\uc678\uc120 \ub178\ucd9c \uacc4\ud68d\uc744 \uc0c1\ub2f4\uc2e4\uc5d0 "
            "\uacf5\uc720\ud574 \uc8fc\uc138\uc694. \ubd89\uc5b4\uc9d0\uc774\ub098 \uc0c9\uc18c\uce68\ucc29 \uc704\ud5d8\uc740 "
            "\ud53c\ubd80 \uc0c1\ud0dc\uc5d0 \ub530\ub77c \ub2e4\ub974\ub2c8 \ud328\uce58/\ud14c\uc2a4\ud2b8 \uc5ec\ubd80\ub97c "
            "\ud655\uc778\ud558\ub294 \uac83\uc774 \uc88b\uc2b5\ub2c8\ub2e4."
        )

    return (
        summary
        + "\n\n"
        + _t(
            "\\uad81\uae08\ud55c \ud56d\ubaa9\uc744 \uc0c9\uc18c, \uc8fc\ub984, \ud0c4\ub825, \uc218\ubd84, "
            "\uc2dc\uc220 \uc6b0\uc120\uc21c\uc704\ucc98\ub7fc \uad6c\uccb4\uc801\uc73c\ub85c \ubb3c\uc5b4\ubcf4\uba74 "
            "\uadf8 \uae30\uc900\uc5d0 \ub9de\ucdb0 \uc0c1\ub2f4 \ubc29\ud5a5\uc744 \uc815\ub9ac\ud574\ub4dc\ub9b4\uac8c\uc694."
        )
    )


def _extract_llm_text(content: Any) -> str:
    if content is None:
        return ""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, dict) and "text" in item:
                parts.append(str(item["text"]))
            else:
                parts.append(str(item))
        return "".join(parts)
    return str(content)


def _update_gpu_utilization() -> None:
    try:
        output = subprocess.check_output(
            [
                "nvidia-smi",
                "--query-gpu=utilization.gpu",
                "--format=csv,noheader,nounits",
            ],
            stderr=subprocess.DEVNULL,
            text=True,
            timeout=1,
        )
        values = [float(line.strip()) for line in output.splitlines() if line.strip()]
        GPU_UTILIZATION.set(max(values) if values else 0)
    except Exception:
        GPU_UTILIZATION.set(0)


def _llm_chat_with_analysis(message: str, analysis: dict[str, Any]) -> str:
    from langchain.chat_models import init_chat_model
    from langchain_core.messages import HumanMessage, SystemMessage

    try:
        from config import AI_MODEL
    except Exception:
        AI_MODEL = "openai:gpt-4o-mini"

    llm = init_chat_model(model=AI_MODEL, temperature=0.2)
    analysis_text = json.dumps(analysis, ensure_ascii=False, indent=2)
    response = llm.invoke(
        [
            SystemMessage(
                content=(
                    "당신은 SkinAI의 피부 분석 상담 어시스턴트입니다. "
                    "이미지 분석 결과가 이미 제공되어 있으므로 image_path를 다시 요구하거나 분석 도구 호출을 요청하지 마세요. "
                    "제공된 분석 수치를 바탕으로 한국어로 간결하고 실용적인 상담 답변을 하세요. "
                    "의학적 진단처럼 단정하지 말고, 실제 시술 여부는 전문가 상담이 필요하다고 안내하세요."
                )
            ),
            HumanMessage(content=f"분석 결과 JSON:\n{analysis_text}\n\n사용자 질문:\n{message}"),
        ]
    )
    return _extract_llm_text(getattr(response, "content", response)).strip()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai"}


@app.get("/metrics")
def metrics() -> Response:
    _update_gpu_utilization()
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.post("/api/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    session_id = request.sessionId or str(uuid.uuid4())
    analysis = request.analysis or _load_latest_analysis()
    openai_key = (os.getenv("OPENAI_API_KEY") or "").strip()
    if not openai_key or openai_key in {"sk-...", "sk-"}:
        return ChatResponse(sessionId=session_id, content=_fallback_chat(request.message, analysis), mode="fallback")

    try:
        if analysis:
            content = _llm_chat_with_analysis(request.message, analysis)
            if content:
                return ChatResponse(sessionId=session_id, content=content, mode="llm")

        from agent.graph import ChatSession

        session = ChatSession(thread_id=session_id)
        content = session.send(request.message)
        if content.strip():
            return ChatResponse(sessionId=session_id, content=content, mode="llm")
    except Exception as exc:  # noqa: BLE001
        print(f"[ai-chat] LLM path failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        fallback = _fallback_chat(request.message, analysis)
        return ChatResponse(sessionId=session_id, content=fallback, mode="llm_error")

    return ChatResponse(sessionId=session_id, content=_fallback_chat(request.message, analysis), mode="fallback")


@app.post("/api/analyze")
async def analyze(
    image: UploadFile = File(...),
    gender: str = Form("female"),
) -> dict[str, Any]:
    inference_start = time.perf_counter()
    if image.content_type and image.content_type not in {"image/jpeg", "image/png", "image/webp"}:
        INFERENCE_COUNT.labels("rejected").inc()
        INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
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
            INFERENCE_COUNT.labels(str(status)).inc()
            INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
            response = {"status": status, "imagePath": str(image_path), "result": result}
            with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
                json.dump(response, file, ensure_ascii=False, indent=2)
            return response
    except Exception as exc:
        INFERENCE_COUNT.labels("fallback").inc()
        INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
        result = _mock_analysis(gender)
        response = {
            "status": "fallback",
            "imagePath": str(image_path),
            "message": f"AI pipeline fallback: {exc}",
            "result": result,
        }
        with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
            json.dump(response, file, ensure_ascii=False, indent=2)
        return response

    INFERENCE_COUNT.labels("fallback").inc()
    INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
    response = {"status": "fallback", "imagePath": str(image_path), "result": _mock_analysis(gender)}
    with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
        json.dump(response, file, ensure_ascii=False, indent=2)
    return response


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
