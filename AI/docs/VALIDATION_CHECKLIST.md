# Validation Checklist for Skin Analysis Pipeline

## Pre-merge gates

- [ ] `python -m py_compile AI/pipeline/quality_gate.py AI/pipeline/preprocess.py AI/pipeline/pipeline.py AI/api.py`
- [ ] QC rejects intentionally blurred/dark/no-face images.
- [ ] API propagates `status: rejected` for rejected images.
- [ ] No model score is shown when `quality.acceptable == false`.

## Dataset leakage tests

- [ ] Assert no `subject_id` overlap across train/val/test.
- [ ] Assert no `capture_session_id` overlap across splits.
- [ ] Report device/site overlap separately.
- [ ] Train background-only classifier to detect leakage.

## Model tests

- [ ] Clean external mobile test.
- [ ] Corruption benchmark: blur, JPEG, low light, overexposure, white-balance shift.
- [ ] Repeated-capture stability.
- [ ] Per subgroup: skin tone, age, sex, device, site.
- [ ] Calibration: ECE, reliability curves, coverage-vs-error.

## RAG and recommendation tests

- [ ] Rule retrieval has versioned source IDs.
- [ ] Vector fallback has similarity threshold and abstain path.
- [ ] Prompt-injection rows cannot modify final answer policy.
- [ ] Recommendations are clinician-reviewed against top concerns.
- [ ] Contraindication questions are asked before procedure-like advice.

## Security/privacy tests

- [ ] CORS restricted in production.
- [ ] Upload size/type validation.
- [ ] Raw image retention disabled by default.
- [ ] Logs do not contain image bytes, PII, or unredacted paths.
- [ ] Deletion path tested.
