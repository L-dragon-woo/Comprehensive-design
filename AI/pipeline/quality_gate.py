"""
Input quality gate for the skin analysis pipeline.

This module is intentionally model-agnostic. It catches mobile-image failure
modes before EfficientNet/TFLite inference: blur, under/over-exposure, tiny
faces, extreme pose, and landmark instability proxies. The pipeline must reject
or down-rank predictions from low-quality images instead of returning confident
skin/age/recommendation scores from garbage input.
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Iterable

import cv2
import numpy as np


@dataclass(frozen=True)
class QualityThresholds:
    min_short_side: int = 480
    min_brightness: float = 45.0
    max_brightness: float = 220.0
    min_laplacian_var: float = 45.0
    min_face_area_ratio: float = 0.12
    max_face_area_ratio: float = 0.85
    max_abs_yaw_rad: float = 0.35
    max_abs_pitch_rad: float = 0.35
    max_abs_roll_rad: float = 0.45
    min_landmark_in_frame_ratio: float = 0.97


DEFAULT_THRESHOLDS = QualityThresholds()


def _as_float(value: float) -> float:
    return float(round(float(value), 4))


def assess_image_quality(
    bgr: np.ndarray,
    thresholds: QualityThresholds = DEFAULT_THRESHOLDS,
) -> dict:
    """Return deterministic image-level QC metrics and failure reasons."""
    if bgr is None or bgr.size == 0:
        return {
            "acceptable": False,
            "reasons": ["image_unreadable"],
            "metrics": {},
        }

    h, w = bgr.shape[:2]
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    brightness = float(np.mean(gray))
    lap_var = float(cv2.Laplacian(gray, cv2.CV_64F).var())

    reasons: list[str] = []
    if min(h, w) < thresholds.min_short_side:
        reasons.append("resolution_too_low")
    if brightness < thresholds.min_brightness:
        reasons.append("under_exposed")
    if brightness > thresholds.max_brightness:
        reasons.append("over_exposed")
    if lap_var < thresholds.min_laplacian_var:
        reasons.append("blur_or_defocus")

    return {
        "acceptable": not reasons,
        "reasons": reasons,
        "metrics": {
            "height": int(h),
            "width": int(w),
            "brightness_mean": _as_float(brightness),
            "laplacian_variance": _as_float(lap_var),
        },
    }


def assess_landmark_quality(
    coords: np.ndarray,
    image_shape: tuple[int, int],
    face_oval_indices: Iterable[int],
    yaw: float,
    pitch: float,
    roll: float,
    thresholds: QualityThresholds = DEFAULT_THRESHOLDS,
) -> dict:
    """Return face/pose/landmark QC metrics and failure reasons."""
    h, w = image_shape[:2]
    reasons: list[str] = []

    if coords is None or len(coords) == 0:
        return {
            "acceptable": False,
            "reasons": ["landmarks_missing"],
            "metrics": {},
        }

    in_frame = (
        (coords[:, 0] >= 0)
        & (coords[:, 0] < w)
        & (coords[:, 1] >= 0)
        & (coords[:, 1] < h)
    )
    in_frame_ratio = float(np.mean(in_frame))

    face_pts = np.array([coords[i] for i in face_oval_indices], dtype=np.int32)
    x, y, fw, fh = cv2.boundingRect(face_pts)
    face_area_ratio = float((fw * fh) / max(1, w * h))

    if face_area_ratio < thresholds.min_face_area_ratio:
        reasons.append("face_too_small")
    if face_area_ratio > thresholds.max_face_area_ratio:
        reasons.append("face_too_close_or_cropped")
    if abs(yaw) > thresholds.max_abs_yaw_rad:
        reasons.append("yaw_too_large")
    if abs(pitch) > thresholds.max_abs_pitch_rad:
        reasons.append("pitch_too_large")
    if abs(roll) > thresholds.max_abs_roll_rad:
        reasons.append("roll_too_large")
    if in_frame_ratio < thresholds.min_landmark_in_frame_ratio:
        reasons.append("face_partially_out_of_frame")
    if not all(math.isfinite(v) for v in (yaw, pitch, roll)):
        reasons.append("pose_not_finite")

    return {
        "acceptable": not reasons,
        "reasons": reasons,
        "metrics": {
            "face_area_ratio": _as_float(face_area_ratio),
            "landmark_in_frame_ratio": _as_float(in_frame_ratio),
            "yaw_rad": _as_float(yaw),
            "pitch_rad": _as_float(pitch),
            "roll_rad": _as_float(roll),
            "face_bbox_xywh": [int(x), int(y), int(fw), int(fh)],
        },
    }


def merge_quality_reports(*reports: dict) -> dict:
    """Merge QC reports into one report consumed by API/RAG layers."""
    reasons: list[str] = []
    metrics: dict = {}
    acceptable = True

    for report in reports:
        if not report:
            continue
        acceptable = acceptable and bool(report.get("acceptable", False))
        reasons.extend(report.get("reasons", []))
        metrics.update(report.get("metrics", {}))

    return {
        "acceptable": acceptable,
        "reasons": sorted(set(reasons)),
        "metrics": metrics,
    }

