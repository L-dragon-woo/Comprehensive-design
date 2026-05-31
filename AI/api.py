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

import yaml
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
    allow_credentials=False,
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


class AnalysisSummaryRequest(BaseModel):
    analysis: dict[str, Any]
    gender: str = "female"
    sessionId: str | None = None


def _t(text: str) -> str:
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
        return f"{label} {value:.1f}점"
    return f"{label} 확인 필요"


def _analysis_summary(analysis: dict[str, Any] | None) -> str:
    if not analysis:
        return _t(
            "\\ud604재 저장된 분석 결과가 없어 "
            "일반적인 상담 기준으로 안내할게요."
        )

    age = analysis.get("age")
    pigment = analysis.get("pigment") or {}
    wrinkle = analysis.get("wrinkle") or {}
    homogenity = analysis.get("homogenity") or {}

    parts: list[str] = []
    if isinstance(age, (int, float)):
        parts.append(_t("\\uc608상 피부 나이 ") + f"{age:.1f}세")
    if pigment:
        parts.append(
            _t("\\uc0c9소는 ")
            + ", ".join(
                [
                    _score_line(_t("\\uc88c측"), pigment.get("left")),
                    _score_line(_t("\\uc6b0측"), pigment.get("right")),
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
                _t("\\uc6b0선 관리가 필요한 주름 부위는 ")
                + ", ".join(f"{key} {value:.1f}점" for key, value in weakest)
            )
    if homogenity:
        parts.append(
            _t("\\ud53c부 균일도는 ")
            + ", ".join(
                [
                    _score_line(_t("\\uad11채"), homogenity.get("radiance")),
                    _score_line(_t("\\uacb0"), homogenity.get("texture")),
                ]
            )
        )
    return " / ".join(parts) if parts else _t("\\ubd84석 값을 읽을 수 없어요.")


def _fallback_chat(message: str, analysis: dict[str, Any] | None = None) -> str:
    normalized = message.lower()
    summary = _analysis_summary(analysis)

    if any(keyword in message for keyword in [_t("\\ub098이"), _t("\\uba87 \\uc0b4"), _t("\\uc5f0령")]) or "age" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\ub098이 추정은 진단값이 아니라 이미지 "
                "기반 모델 추정치입니다. 조명, 표정, "
                "화장에 따라 달라질 수 있어서 참고용으로만 "
                "보는 것이 좋습니다."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc0c9소"), _t("\\uc7a1티"), _t("\\uae30미"), _t("\\ud53c코")]) or "pigment" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc0c9소 고민은 피코토닝 계열 상담을 "
                "우선 고려할 수 있습니다. 다만 자외선 "
                "노출, 최근 레이저 이력, 피부 민감도에 "
                "따라 강도와 주기를 조절해야 합니다."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc8fc름"), _t("\\ud0c4력"), _t("\\ub9ac쥬란"), _t("\\ud68c복")]) or "wrinkle" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc8fc름과 탄력 고민은 리쥬란, 스킨부스터, "
                "탄력 장비 상담을 비교해볼 수 있습니다. "
                "회복 기간이 짧아야 한다면 강도를 낮춰 "
                "단계적으로 진행하는 편이 안전합니다."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc6b0선"), _t("\\uba3c저"), _t("\\uc21c위"), _t("\\ucd94천")]) or "priority" in normalized:
        return (
            summary
            + "\n\n"
            + _t(
                "\\uc6b0선순위는 1) 피부 장벽과 수분 관리, "
                "2) 색소 상담, 3) 탄력/주름 상담 순서가 "
                "무난합니다. 한 번에 강한 시술을 묶기보다 "
                "반응을 보면서 단계적으로 진행하는 것을 "
                "추천합니다."
            )
        )

    if any(keyword in message for keyword in [_t("\\uc8fc의"), _t("\\ubd80작용"), _t("\\ud655인"), _t("\\ud558면 \\uc548")]) or "risk" in normalized:
        return _t(
            "\\uc2dc술 전에는 최근 시술 이력, 알레르기, "
            "복용 중인 약, 임신 가능성, 콘로이드 체질, "
            "탄닝 및 자외선 노출 계획을 상담실에 "
            "공유해 주세요. 붉어짐이나 색소침착 위험은 "
            "피부 상태에 따라 다르니 패치/테스트 여부를 "
            "확인하는 것이 좋습니다."
        )

    return (
        summary
        + "\n\n"
        + _t(
            "\\uad81금한 항목을 색소, 주름, 탄력, 수분, "
            "시술 우선순위처럼 구체적으로 물어보면 "
            "그 기준에 맞춰 상담 방향을 정리해드릴게요."
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
            ["nvidia-smi", "--query-gpu=utilization.gpu", "--format=csv,noheader,nounits"],
            stderr=subprocess.DEVNULL,
            text=True,
            timeout=1,
        )
        values = [float(line.strip()) for line in output.splitlines() if line.strip()]
        GPU_UTILIZATION.set(max(values) if values else 0)
    except Exception:
        GPU_UTILIZATION.set(0)


def _runtime_config_path() -> Path:
    """pipeline/config.yaml의 root_dir을 실제 pipeline/ 경로로 덮어쓴 사본을 반환."""
    with open(PIPELINE_DIR / "config.yaml", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)
    cfg["root_dir"] = str(PIPELINE_DIR)
    cache_dir = ROOT / ".cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    out = cache_dir / "pipeline_runtime_config.yaml"
    with open(out, "w", encoding="utf-8") as f:
        yaml.safe_dump(cfg, f, allow_unicode=True)
    return out


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


def _pipeline_result_from_analysis(analysis: dict[str, Any]) -> dict[str, Any]:
    nested = analysis.get("result")
    return nested if isinstance(nested, dict) else analysis


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
    except Exception as exc:
        print(f"[ai-chat] LLM path failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        fallback = _fallback_chat(request.message, analysis)
        return ChatResponse(sessionId=session_id, content=fallback, mode="llm_error")

    return ChatResponse(sessionId=session_id, content=_fallback_chat(request.message, analysis), mode="fallback")


@app.post("/api/analyses/summary")
def analyses_summary(request: AnalysisSummaryRequest) -> dict[str, Any]:
    """피부 분석 JSON에서 beauty-agent(PubMed 포함)를 이용해 종합 레포트 생성."""
    session_id = request.sessionId or str(uuid.uuid4())
    pipeline_result = _pipeline_result_from_analysis(request.analysis or {})
    gender = (request.gender or "female").lower()

    openai_key = (os.getenv("OPENAI_API_KEY") or "").strip()
    if not openai_key or openai_key in {"sk-...", "sk-"}:
        return {"content": _fallback_chat("레포트 작성해줘", pipeline_result), "sessionId": session_id, "mode": "fallback"}

    try:
        from tools.skin_analyze import _flatten, aggregate_regions
        from agent.graph import ChatSession

        flat_scores = _flatten(pipeline_result)
        has_any = any(v is not None for v in flat_scores.values())

        if not has_any:
            content = _llm_chat_with_analysis("피부 분석 결과를 한국어로 읽기 쉽게 요약해줘.", pipeline_result)
            return {"content": content, "sessionId": session_id, "mode": "llm"}

        regions = aggregate_regions(flat_scores)
        top_concerns = regions[:3]
        skin_scores = {
            "raw_scores": flat_scores,
            "age": pipeline_result.get("age"),
            "gender_input": gender,
            "valid_sagging": pipeline_result.get("valid_sagging"),
        }

        session = ChatSession(thread_id=session_id)
        session.graph.update_state(session.config, {
            "skin_scores": skin_scores,
            "top_concerns": top_concerns,
            "gender": gender,
        })

        # 1단계: 추천 + PubMed 근거 수집 (inject_recommend / inject_pubmed 자동 발동)
        try:
            session.send(
                f"The skin analysis is complete and the user's gender is {gender}. "
                "Recommend treatment by skin region using the stored analysis state."
            )
        except Exception as exc:
            print(f"[summary] recommend step failed: {type(exc).__name__}: {exc}", file=sys.stderr)

        # 2단계: 종합 레포트 생성 (final_report 노드 경유)
        session.send("Write the final report summary. Include PubMed evidence and PMID when available.")

        final_text = session.final_answer
        if not final_text or not final_text.strip():
            final_text = _llm_chat_with_analysis("피부 분석 결과를 한국어로 요약해줘.", pipeline_result)

        return {
            "content": final_text.strip(),
            "sessionId": session_id,
            "mode": "agent_pubmed" if session.pubmed_recommendations else "agent",
        }

    except Exception as exc:
        print(f"[summary] agent failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        try:
            content = _llm_chat_with_analysis("피부 분석 결과를 한국어로 요약해줘.", pipeline_result)
            return {"content": content, "sessionId": session_id, "mode": "llm_fallback"}
        except Exception:
            return {"content": _fallback_chat("레포트", pipeline_result), "sessionId": session_id, "mode": "fallback"}


_ALLOWED_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
_MAX_UPLOAD_BYTES = 10 * 1024 * 1024  # 10 MB — matches backend Spring multipart limit


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

    suffix = Path(image.filename or "image.jpg").suffix.lower() or ".jpg"
    if suffix not in _ALLOWED_SUFFIXES:
        INFERENCE_COUNT.labels("rejected").inc()
        INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
        return {"status": "rejected", "message": "Unsupported file extension.", "result": None}

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    image_path = UPLOAD_DIR / f"{uuid.uuid4()}{suffix}"

    # Stream upload with size cap to avoid disk exhaustion
    written = 0
    with image_path.open("wb") as target:
        while chunk := await image.read(1024 * 64):
            written += len(chunk)
            if written > _MAX_UPLOAD_BYTES:
                target.close()
                image_path.unlink(missing_ok=True)
                INFERENCE_COUNT.labels("rejected").inc()
                INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
                return {"status": "rejected", "message": "Upload too large (max 10 MB).", "result": None}
            target.write(chunk)

    try:
        from pipeline import SkinPipeline

        pipe = SkinPipeline(_runtime_config_path(), gender=gender)
        result = pipe.predict_single(str(image_path))

        if result is None:
            INFERENCE_COUNT.labels("face_not_detected").inc()
            INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
            response = {
                "status": "face_not_detected",
                "imagePath": str(image_path),
                "message": "얼굴을 검출하지 못했습니다. 정면 얼굴 사진을 다시 업로드해 주세요.",
                "result": None,
            }
            with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
                json.dump(response, file, ensure_ascii=False, indent=2)
            return response

        INFERENCE_COUNT.labels("completed").inc()
        INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
        response = {"status": "completed", "imagePath": str(image_path), "result": result}
        with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
            json.dump(response, file, ensure_ascii=False, indent=2)
        return response

    except Exception as exc:
        print(f"[ai-analyze] pipeline error: {type(exc).__name__}: {exc}", file=sys.stderr)
        INFERENCE_COUNT.labels("fallback").inc()
        INFERENCE_DURATION.observe(time.perf_counter() - inference_start)
        result = _mock_analysis(gender)
        response = {
            "status": "fallback",
            "imagePath": str(image_path),
            "message": "AI 파이프라인 오류로 기본 결과를 반환합니다.",
            "result": result,
        }
        with LAST_ANALYSIS_PATH.open("w", encoding="utf-8") as file:
            json.dump(response, file, ensure_ascii=False, indent=2)
        return response
    finally:
        image_path.unlink(missing_ok=True)
