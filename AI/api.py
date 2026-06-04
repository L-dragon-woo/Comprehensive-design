from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import threading
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
_PIPELINE_CACHE: dict[str, Any] = {}
_PIPELINE_CACHE_LOCK = threading.Lock()
_PIPELINE_READY = False
_PIPELINE_PRELOAD_ERROR: str | None = None

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
    history: list[dict[str, Any]] | None = None


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

    if "리쥬란" in message or "rejuran" in normalized:
        return (
            "리쥬란은 피부 속 회복 성분인 PN/PDRN 계열 성분을 주사해서 피부결, 탄력, 잔주름, 장벽 회복을 돕는 시술입니다.\n\n"
            "쉽게 말하면 꺼진 부위를 채우는 필러라기보다, 피부 컨디션을 천천히 끌어올리는 재생 주사에 가깝습니다. "
            "효과는 보통 즉시 드라마틱하게 보이기보다 몇 주에 걸쳐 피부결과 탄력이 완만하게 좋아지는 쪽입니다.\n\n"
            "현재 분석에서 주름/탄력 쪽을 같이 상담해볼 수 있으니, 병원에서는 리쥬란 단독이 맞는지 스킨부스터나 탄력 장비와 병행이 나은지 확인해 보시면 좋습니다."
        )

    if "피코" in message or "토닝" in message or "picotoning" in normalized or "pico" in normalized:
        return (
            "피코토닝은 짧은 시간에 강한 레이저 에너지를 쏘아 색소를 잘게 분해하는 색소 레이저 계열 시술입니다.\n\n"
            "기미, 잡티, 색소침착처럼 색소 고민이 있을 때 상담하는 경우가 많고, 한 번에 세게 하기보다 피부 반응을 보면서 여러 번 나누어 진행하는 편이 일반적입니다.\n\n"
            "시술 전에는 최근 레이저 이력, 햇빛 노출, 피부 민감도, 기미 여부를 꼭 같이 확인해야 합니다."
        )

    if "스킨부스터" in message or "skinbooster" in normalized or "skin booster" in normalized:
        return (
            "스킨부스터는 피부 속에 수분감이나 탄력 개선 성분을 얕게 주입해서 피부결, 광채, 건조감을 개선하는 시술군입니다.\n\n"
            "리쥬란도 넓게 보면 스킨부스터 계열로 설명되는 경우가 있지만, 병원마다 제품과 목적이 다릅니다. "
            "건조함과 피부결이 중심이면 스킨부스터, 잔주름과 회복/탄력까지 같이 보면 리쥬란을 비교해볼 수 있습니다."
        )

    if "보톡스" in message or "botox" in normalized or "톡신" in message:
        return (
            "보톡스는 근육 움직임을 일시적으로 줄여 표정 주름을 완화하는 주사 시술입니다.\n\n"
            "이마, 미간, 눈가처럼 표정에 따라 접히는 주름에는 도움이 될 수 있지만, 피부 자체의 탄력 저하나 꺼짐에는 리쥬란, 스킨부스터, 리프팅, 필러 같은 다른 선택지가 더 맞을 수 있습니다."
        )

    if "필러" in message or "filler" in normalized:
        return (
            "필러는 꺼진 부위나 볼륨이 부족한 부위에 주입해서 모양을 보완하는 시술입니다.\n\n"
            "볼꺼짐, 팔자 부위, 입가 라인처럼 구조적인 꺼짐이 뚜렷할 때 상담할 수 있습니다. "
            "다만 과교정이나 혈관 관련 부작용 가능성이 있어, 반드시 경험 있는 의료진과 용량과 위치를 보수적으로 정하는 것이 중요합니다."
        )

    if any(keyword in message for keyword in ["차이", "비교", "중 무엇", "뭐가 좋아", "뭐가 나아"]):
        return (
            "간단히 비교하면 목적이 다릅니다.\n\n"
            "리쥬란은 피부결, 잔주름, 탄력, 회복을 천천히 개선하는 재생 주사 쪽이고, "
            "피코토닝은 잡티나 색소침착을 줄이는 색소 레이저 쪽입니다.\n\n"
            "색소가 가장 신경 쓰이면 피코토닝 상담을 먼저, 피부결과 잔주름이 더 신경 쓰이면 리쥬란이나 스킨부스터 상담을 먼저 잡는 편이 자연스럽습니다."
        )

    if any(keyword in message for keyword in ["추천", "우선", "먼저", "순위"]) or "priority" in normalized:
        return (
            f"분석 요약을 기준으로 보면 {summary}입니다.\n\n"
            "상담 우선순위는 1) 피부 장벽과 수분 관리, 2) 색소 레이저 상담, 3) 주름/탄력 시술 상담 순서가 무난합니다.\n\n"
            "한 번에 여러 시술을 강하게 묶기보다, 가장 신경 쓰이는 고민 하나를 먼저 정하고 피부 반응을 보면서 단계적으로 진행하는 쪽이 안전합니다."
        )

    if any(keyword in message for keyword in ["색소", "잡티", "기미"]) or "pigment" in normalized:
        return (
            "색소 고민은 피코토닝, IPL, 미백 관리 같은 색소 계열 상담을 먼저 고려할 수 있습니다.\n\n"
            "다만 기미가 섞여 있거나 피부가 예민하면 강한 레이저가 오히려 자극이 될 수 있어서, 병원에서 색소 종류와 최근 햇빛 노출 여부를 먼저 확인하는 게 좋습니다."
        )

    if any(keyword in message for keyword in ["주름", "탄력", "회복"]) or "wrinkle" in normalized:
        return (
            "주름과 탄력 고민은 원인에 따라 선택지가 달라집니다.\n\n"
            "표정 때문에 접히는 주름은 보톡스, 피부결과 잔주름은 리쥬란이나 스킨부스터, 처짐이 중심이면 리프팅 장비 상담이 더 잘 맞을 수 있습니다."
        )

    if any(keyword in message for keyword in ["주의", "부작용", "확인", "하면 안"]) or "risk" in normalized:
        return (
            "시술 전에는 최근 시술 이력, 알레르기, 복용 중인 약, 임신 가능성, 켈로이드 체질, 햇빛 노출 계획을 상담실에 공유해 주세요.\n\n"
            "붉어짐, 멍, 색소침착, 건조감은 피부 상태에 따라 달라질 수 있으니 회복 기간과 사후관리 방법도 같이 확인하는 것이 좋습니다."
        )

    if any(keyword in message for keyword in ["나이", "몇 살", "연령"]) or "age" in normalized:
        return (
            f"{summary}\n\n"
            "피부 나이는 진단명이 아니라 이미지 기반 추정값입니다. 조명, 표정, 화장 상태에 따라 달라질 수 있어서 참고용으로 보는 것이 좋습니다."
        )

    return (
        "지금은 OpenAI 호출 한도 때문에 간단 상담 모드로 답변하고 있습니다.\n\n"
        "리쥬란, 피코토닝, 스킨부스터, 보톡스, 필러처럼 궁금한 시술명을 물어보면 목적, 차이, 주의사항 위주로 바로 설명해드릴게요."
    )


def _score_value(value: Any) -> float | None:
    if isinstance(value, (int, float)):
        return float(value)
    return None


def _score_phrase(score: float | None) -> str:
    if score is None:
        return "확인 필요"
    if score >= 85:
        return "양호"
    if score >= 70:
        return "관찰 필요"
    return "우선 관리 필요"


def _average_scores(values: list[Any]) -> float | None:
    nums = [_score_value(value) for value in values]
    nums = [value for value in nums if value is not None]
    return round(sum(nums) / len(nums), 1) if nums else None


def _deterministic_report(analysis: dict[str, Any], gender: str) -> str:
    """Return a card-friendly report when the LLM agent cannot finish."""
    if analysis.get("status") == "rejected":
        quality = analysis.get("quality") or {}
        reasons = quality.get("reasons") if isinstance(quality, dict) else None
        reason_text = ", ".join(str(reason) for reason in reasons) if isinstance(reasons, list) else "촬영 품질 기준 미달"
        return (
            "# AI 종합 분석\n\n"
            "[Step 1] 피부 진단 결과\n"
            f"- **촬영 품질: 재촬영 필요**\n  현재 이미지는 {reason_text} 항목 때문에 정량 분석 신뢰도가 낮습니다.\n\n"
            "[Step 2] 추천 방향\n"
            "- **재촬영 우선**\n  정면 얼굴이 화면 중앙에 크게 보이도록 하고, 흔들림이 없게 밝은 환경에서 다시 촬영해 주세요.\n\n"
            "[Step 3] 학술 근거\n"
            "정량 점수가 확보되지 않아 논문 근거 기반 시술 추천은 보류했습니다."
        )

    age = _score_value(analysis.get("age"))
    pigment = analysis.get("pigment") or {}
    wrinkle = analysis.get("wrinkle") or {}
    homogenity = analysis.get("homogenity") or {}
    sagging = [
        (analysis.get("cheek_sagging") or {}).get("total"),
        (analysis.get("chin_sagging") or {}).get("total"),
    ]

    pigment_avg = _average_scores([pigment.get("left"), pigment.get("right")])
    wrinkle_avg = _average_scores(list(wrinkle.values()) if isinstance(wrinkle, dict) else [])
    texture_avg = _average_scores([homogenity.get("radiance"), homogenity.get("texture")])
    sagging_avg = _average_scores(sagging)

    diagnosis_items: list[tuple[str, float | None, str]] = [
        ("색소", pigment_avg, "좌우 색소 점수를 기준으로 잡티와 색소침착 관리 우선도를 판단했습니다."),
        ("주름", wrinkle_avg, "이마, 눈가, 팔자, 입가 턱, 볼꺼짐 점수를 함께 반영했습니다."),
        ("피부결/광채", texture_avg, "광채와 거칠기 점수로 피부 균일도와 장벽 컨디션을 봤습니다."),
    ]
    if sagging_avg is not None:
        diagnosis_items.append(("탄력/처짐", sagging_avg, "볼과 턱 라인의 처짐 지표를 함께 확인했습니다."))

    ranked = sorted(
        [item for item in diagnosis_items if item[1] is not None],
        key=lambda item: item[1] or 101,
    )
    top = ranked[:3] if ranked else diagnosis_items[:3]

    lines = ["# AI 종합 분석", "", "[Step 1] 피부 진단 결과"]
    if age is not None:
        lines.append(f"- **예상 피부 나이: {age:.1f}세**")
        lines.append("  사진 기반 추정값이므로 조명, 표정, 화장 상태에 따라 달라질 수 있습니다.")
    for label, score, note in diagnosis_items:
        score_text = f"{score:.1f}점" if score is not None else "확인 필요"
        lines.append(f"- **{label}: {score_text}**")
        lines.append(f"  {_score_phrase(score)} 상태입니다. {note}")

    lines.extend(["", "[Step 2] 추천 시술 및 관리 우선순위"])
    treatment_map = {
        "색소": "색소 점수가 낮거나 좌우 차이가 크면 피코토닝, IPL, 미백 관리 상담을 우선 고려할 수 있습니다.",
        "주름": "주름 점수가 낮은 부위는 보툴리눔 톡신, 스킨부스터, 리쥬란, 탄력 장비 상담을 비교해볼 수 있습니다.",
        "피부결/광채": "피부결과 광채는 수분 장벽 관리, 스킨부스터, 진정 관리처럼 회복 부담이 낮은 방향부터 시작하는 편이 좋습니다.",
        "탄력/처짐": "처짐이 관찰되면 리프팅 장비, 콜라겐 부스터, 윤곽 상담을 단계적으로 검토할 수 있습니다.",
    }
    for label, score, _ in top:
        score_text = f"{score:.1f}점" if score is not None else "확인 필요"
        lines.append(f"- **{label} ({score_text})**")
        lines.append(f"  {treatment_map.get(label, '전문의 상담으로 관리 우선순위를 정하는 것이 좋습니다.')}")

    lines.extend([
        "",
        "[Step 3] 학술 근거",
        "현재 OpenAI 호출 한도 문제로 beauty-agent의 PubMed 근거 생성까지 완료하지 못했습니다.",
        "따라서 이 리포트는 저장된 AI 분석 점수와 내부 시술 규칙을 기준으로 만든 임시 종합 분석이며, 실제 시술 결정은 피부과 전문의 상담으로 확정해야 합니다.",
    ])
    return "\n".join(lines).strip()


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


def _get_skin_pipeline(gender: str):
    normalized_gender = (gender or "female").strip().lower()
    if normalized_gender not in {"male", "female"}:
        normalized_gender = "female"

    with _PIPELINE_CACHE_LOCK:
        pipe = _PIPELINE_CACHE.get(normalized_gender)
        if pipe is None:
            from pipeline import SkinPipeline

            pipe = SkinPipeline(_runtime_config_path(), gender=normalized_gender)
            _PIPELINE_CACHE[normalized_gender] = pipe
        return pipe


def _truthy_env(name: str, default: str = "true") -> bool:
    return os.getenv(name, default).strip().lower() not in {"0", "false", "no", "off"}


@app.on_event("startup")
def preload_default_pipeline() -> None:
    global _PIPELINE_PRELOAD_ERROR, _PIPELINE_READY

    if not _truthy_env("AI_PRELOAD_PIPELINE"):
        _PIPELINE_READY = True
        return

    gender = os.getenv("AI_DEFAULT_GENDER", "female")
    try:
        _get_skin_pipeline(gender)
        _PIPELINE_PRELOAD_ERROR = None
        _PIPELINE_READY = True
    except Exception as exc:
        _PIPELINE_PRELOAD_ERROR = f"{type(exc).__name__}: {exc}"
        _PIPELINE_READY = False
        print(f"[startup] pipeline preload failed: {_PIPELINE_PRELOAD_ERROR}", file=sys.stderr)


def _format_chat_history(history: list[dict[str, Any]] | None) -> str:
    if not history:
        return "No previous conversation"

    lines: list[str] = []
    for item in history[-8:]:
        role = str(item.get("role", "")).lower()
        content = str(item.get("content", "")).strip()
        if not content:
            continue
        label = "User" if role == "user" else "Assistant"
        lines.append(f"{label}: {content[:700]}")
    return "\n".join(lines) if lines else "No previous conversation"


def _llm_chat_with_analysis(message: str, analysis: dict[str, Any], history: list[dict[str, Any]] | None = None) -> str:
    from langchain.chat_models import init_chat_model
    from langchain_core.messages import HumanMessage, SystemMessage

    try:
        from config import AI_MODEL
    except Exception:
        AI_MODEL = "openai:gpt-4o-mini"

    llm = init_chat_model(model=AI_MODEL, temperature=0.2)
    analysis_text = json.dumps(analysis, ensure_ascii=False, indent=2)
    history_text = _format_chat_history(history)
    response = llm.invoke(
        [
            SystemMessage(
                content=(
                    "You are SkinAI's Korean skin-analysis consultation assistant. "
                    "Use the provided analysis JSON and previous conversation to answer the current question. "
                    "Do not request a new image or call image analysis again. "
                    "Do not greet again after the first assistant message. "
                    "Avoid repeating the same explanation from earlier turns; answer only the new or missing part. "
                    "Keep answers practical, concise, and in Korean. "
                    "Do not present medical certainty; advise an in-person professional consultation for final treatment decisions."
                )
            ),
            HumanMessage(content=f"Analysis JSON:\n{analysis_text}\n\nPrevious conversation:\n{history_text}\n\nCurrent user question:\n{message}"),
        ]
    )
    return _extract_llm_text(getattr(response, "content", response)).strip()

def _pipeline_result_from_analysis(analysis: dict[str, Any]) -> dict[str, Any]:
    nested = analysis.get("result")
    return nested if isinstance(nested, dict) else analysis


def _strip_qna_section(content: str) -> str:
    """Remove optional QnA/follow-up sections from generated analysis reports."""
    lines = content.replace("\r\n", "\n").split("\n")
    kept: list[str] = []
    skipping = False

    for line in lines:
        normalized = line.strip().lower()
        starts_step = re.match(r"^#{0,6}\s*\[?\s*step\s*\d+\s*\]?", normalized)
        is_qna = (
            re.match(r"^#{0,6}\s*\[?\s*step\s*4\s*\]?", normalized) is not None
            or "qna" in normalized
            or "q&a" in normalized
            or "질문" in normalized
            or "답변" in normalized
        )

        if is_qna:
            skipping = True
            continue

        if skipping and starts_step:
            skipping = False

        if not skipping:
            kept.append(line)

    return "\n".join(kept).strip()


def _is_insufficient_report(content: str) -> bool:
    """Detect agent follow-up prompts that should not be saved as a report."""
    normalized = content.strip()
    return (
        "최종 레포트를 작성하려면" in normalized
        or "지금까지 완료된 단계" in normalized
        or "어떤 부분부터 진행하시겠어요" in normalized
        or "사진을 업로드해 주시거나" in normalized
    )


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


@app.get("/ready", response_model=None)
def ready() -> Any:
    if not _PIPELINE_READY:
        payload = {"status": "starting", "service": "ai"}
        if _PIPELINE_PRELOAD_ERROR:
            payload["error"] = _PIPELINE_PRELOAD_ERROR
        return Response(json.dumps(payload), status_code=503, media_type="application/json")
    return {"status": "ready", "service": "ai"}


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
            content = _llm_chat_with_analysis(request.message, analysis, request.history)
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
        return {"content": _deterministic_report(pipeline_result, gender), "sessionId": session_id, "mode": "deterministic_report"}

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
        session.send(
            "Write the final skin analysis report summary only. "
            "Include diagnosis, recommended treatments, care guidance, and PubMed evidence with PMID when available. "
            "Do not include QnA, FAQ, follow-up questions, or any Step 4 question-answer section."
        )

        final_text = session.final_answer
        if not final_text or not final_text.strip():
            final_text = _llm_chat_with_analysis("피부 분석 결과를 한국어로 요약해줘.", pipeline_result)
        final_text = _strip_qna_section(final_text).strip()
        if _is_insufficient_report(final_text):
            final_text = _deterministic_report(pipeline_result, gender)

        return {
            "content": final_text,
            "sessionId": session_id,
            "mode": "agent_pubmed" if session.pubmed_recommendations else "agent",
        }

    except Exception as exc:
        print(f"[summary] agent failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        try:
            content = _llm_chat_with_analysis("피부 분석 결과를 한국어로 요약해줘.", pipeline_result)
            return {"content": content, "sessionId": session_id, "mode": "llm_fallback"}
        except Exception:
            return {"content": _deterministic_report(pipeline_result, gender), "sessionId": session_id, "mode": "deterministic_report"}


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
        pipe = _get_skin_pipeline(gender)
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

