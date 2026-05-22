# Critical QA and Redesign: Skin VLM / CV / RAG System

This document is intentionally blunt. The current system is not production-ready for medical-like skin assessment. It can be used as an internal prototype only after the gates and experiments below are implemented.

## Executive Summary

The repository contains a useful prototype: MediaPipe-based face preprocessing, EfficientNetV2/TFLite task models, sagging geometric predictors, AuraDB retrieval, and a LangGraph recommendation agent. The failure mode is also obvious: the pipeline turns uncontrolled smartphone images into deterministic-looking scores and then recommendations without enough input QC, uncertainty, leakage controls, demographic validation, or retrieval grounding.

Most critical fixes:

1. Add hard input quality gate before model inference. Implemented in `AI/pipeline/quality_gate.py` and wired into `preprocess.py`/`pipeline.py`.
2. Stop treating segmentation/masking as truth. Run ablations: raw face, soft mask, hard mask, background-only, black-mask sentinel.
3. Split data by subject/device/site/time, not image. Current JSON-based left/right crops can leak same person into train/val if split generation is not subject-locked.
4. Replace single-task EfficientNet heads with a calibrated multi-task model plus uncertainty, or keep task models but add OOD/reject logic and calibration.
5. Treat RAG output as non-medical support content with citations, versioned rules, confidence, contraindication guardrails, and prompt-injection isolation.

## Strengths

- Modular pipeline: preprocessing, TFLite inference, sagging predictor, AuraDB recommendation layer are separated.
- TFLite models make deployment feasible on CPU/GPU constrained services.
- Region-specific wrinkle and pigment crops encode useful dermatology priors.
- Agent tools maintain raw scores in state and only pass summaries to the LLM; this is better than letting the LLM invent scores.
- Some augmentation exists for brightness/contrast/noise, which targets smartphone variation.

## Critical Issues

### Issue 1: No hard input quality gate before inference

#### Problem
The previous pipeline ran model inference on low-light, blurred, tiny-face, cropped-face, or extreme-pose images. MediaPipe success was treated as sufficient.

#### Root Cause
Preprocessing was designed as crop generation, not as data validity assessment. There was no explicit accept/reject contract.

#### Impact
Confident but meaningless scores. Unsafe recommendations. User trust collapse. Product liability because the UI may present low-quality output as analysis.

#### Fix Proposal
Reject or downgrade poor inputs before model inference. Return structured reasons and metrics.

#### Redesigned Version
Implemented:
- `AI/pipeline/quality_gate.py`
- `FacePreprocessor.last_quality_report`
- `SkinPipeline.predict_single()` returns `status: rejected` with `quality` and `reject_reason`
- API propagates rejected status

#### Validation Experiment
Create a 500-image QC set with labels: acceptable, blur, dark, overexposed, occluded, extreme yaw, tiny face, cropped face. Report gate precision/recall. Target: reject recall >= 95% for severe failures, false reject <= 10% for acceptable images.

---

### Issue 2: Hard skin masking can create shortcut artifacts

#### Problem
The preprocessing zeros eyes, brows, lips, and background. Models may learn black-mask geometry, crop padding, or mask-edge artifacts rather than pigmentation/wrinkle texture.

#### Root Cause
Hard mask is treated as denoising, but it changes the image distribution and creates synthetic boundaries correlated with pose, camera, sex, age, and label pipelines.

#### Impact
High internal validation, poor smartphone generalization. Shortcut learning. Failure on makeup, occlusions, facial hair, glasses, masks, and non-Korean/underrepresented skin tones.

#### Fix Proposal
Move from hard masking to multi-view training: raw aligned face + soft skin probability mask + mask channel. Never black out pixels without ablation proof.

#### Redesigned Version
Input tensor options:
- RGB aligned crop
- optional soft skin mask as 4th channel
- optional uncertainty map for landmark/mask confidence
- random mask dropout during training

#### Validation Experiment
Train/evaluate four arms: raw, hard mask, soft mask, raw+mask-channel. Add background-only and mask-edge-only controls. If background-only AUC/accuracy is high, labels leak through capture context.

---

### Issue 3: ROI crops are brittle and landmark-index dependent

#### Problem
Pigment crops use fixed landmark pairs. Wrinkle crops use hard-coded rectangles and overlap boxes. Small landmark shifts change the ROI drastically.

#### Root Cause
No ROI stability metric, no crop validity check, no multi-crop aggregation, no fallback crop generation.

#### Impact
Inference is unstable under expression, camera angle, face shape, landmark jitter, and partial occlusion. Same user can get different recommendations across frames.

#### Fix Proposal
Use canonical UV face mapping or normalized mesh coordinates. For every region, generate multiple jittered crops and aggregate predictions with variance.

#### Redesigned Version
For each ROI:
- canonical face-space polygon
- soft boundary expansion
- N=5 jittered crops during inference
- output mean, std, crop_validity
- reject if std too high or crop area too small

#### Validation Experiment
Take 100 users with 5 repeated smartphone photos. Measure intra-user coefficient of variation per score. Target CV < 10% for acceptable images.

---

### Issue 4: Data split leakage risk

#### Problem
Current dataloaders flatten left/right crops into samples. If split JSON files were generated at image/crop level, same subject can appear in train and val/test.

#### Root Cause
The split contract is implicit. Dataloader trusts JSON and does not assert subject exclusivity.

#### Impact
Inflated validation metrics. CVPR/medical reviewer rejection. Deployment surprise because test performance collapses on new users/devices.

#### Fix Proposal
Enforce split manifests with subject_id, capture_session_id, device_id, site_id, timestamp. Add a pre-training assertion: no subject/session/device leakage across splits.

#### Redesigned Version
Dataset manifest schema:
```json
{
  "sample_id": "...",
  "subject_id": "...",
  "capture_session_id": "...",
  "device_model": "...",
  "site": "hospital|mobile",
  "fitzpatrick_or_ita": "...",
  "image_path": "...",
  "region_labels": {...},
  "labeler_ids": [...],
  "split": "train|val|test|external_test"
}
```

#### Validation Experiment
Run subject-disjoint, device-disjoint, site-disjoint, and time-forward test sets. Report all; do not cherry-pick random split.

---

### Issue 5: Label quality is underspecified

#### Problem
“Dermatologist labeled dataset” is not enough. No visible labeler agreement, adjudication protocol, severity rubric, uncertainty, or weak-label handling.

#### Root Cause
Labels are treated as ground truth scalars/classes, not noisy clinical observations.

#### Impact
Model learns annotator bias. Regression scores imply precision that labels may not support.

#### Fix Proposal
Store labeler_id, rubric_version, raw votes, adjudicated label, confidence, and reason. Train with ordinal/noisy-label losses.

#### Redesigned Version
Use ordinal regression for severity, not plain categorical softmax. Use label distribution learning when multiple dermatologists disagree.

#### Validation Experiment
Measure Cohen/Fleiss kappa, intra-class correlation, and per-region label entropy. Compare hard-label vs soft-label training.

---

### Issue 6: EfficientNetV2S single-task design is not enough

#### Problem
Separate models for pigment, wrinkle, age, homogenity duplicate features and do not model uncertainty or task correlation.

#### Root Cause
Engineering convenience dominated over representation design.

#### Impact
Higher latency/memory, inconsistent outputs, no shared calibration. Age prediction can leak demographic priors into downstream recommendation.

#### Fix Proposal
Build a shared encoder with region tokens and task heads, or keep TFLite task models but add a meta-calibration layer.

#### Redesigned Version
Recommended architecture:
- image encoder: ConvNeXtV2/EfficientNetV2/ViT-small trained self-supervised on in-domain face crops
- region tokenizer: ROIAlign/canonical mesh crops
- heads: pigment ordinal, wrinkle ordinal per region, texture/radiance regression, age interval with uncertainty
- calibration: temperature scaling/isotonic per demographic group
- OOD head: image-quality + domain classifier + embedding distance

#### Validation Experiment
Compare current separate TFLite models vs multi-task shared encoder vs foundation-feature linear probes on external smartphone test.

---

### Issue 7: Training does not prove robustness

#### Problem
Augmentation covers some HSV and noise changes, but not enough: blur, JPEG compression, shadows, white balance, makeup, sunscreen shine, occlusion, expression, glasses, camera models.

#### Root Cause
Augmentation list is hand-picked and not tied to measured deployment failures.

#### Impact
Mobile production fails exactly where users are noisy.

#### Fix Proposal
Use deployment-informed augmentation and corruption benchmarks.

#### Redesigned Version
Training augmentations:
- low-light/high-light, white balance shift, gamma, JPEG quality, Gaussian/motion blur
- specular highlights/oily shine
- occlusion cutouts for hair/glasses/mask/fingers
- camera pipeline simulation and resizing artifacts
- skin-tone preserving color jitter with ITA/skin-tone audits

#### Validation Experiment
Report clean test plus corruption mCE-style score. Require bounded degradation per corruption.

---

### Issue 8: Evaluation design is missing reviewer-grade proof

#### Problem
No clear external validation, calibration, subgroup fairness, repeated-capture stability, or clinical utility analysis.

#### Root Cause
Training scripts report accuracy/AUC, but medical-like product needs more.

#### Impact
Cannot claim reliability, safety, or fairness.

#### Fix Proposal
Add locked external test sets and confidence intervals.

#### Redesigned Version
Minimum metrics:
- classification: macro-F1, balanced accuracy, AUROC, AUPRC, ECE/MCE calibration
- regression: MAE, RMSE, Spearman, ICC, Bland-Altman
- ordinal: quadratic weighted kappa
- fairness: per skin tone/age/sex/device/site metrics
- stability: repeated-photo variance
- reject-option: coverage vs accuracy curve

#### Validation Experiment
Bootstrap 95% CIs and pre-register acceptance thresholds.

---

### Issue 9: RAG/recommendation layer can be unsafe

#### Problem
AuraDB rules/vector fallback can return stale or weakly matched procedures. LLM can phrase treatment suggestions as medical advice. Prompt injection can target tool outputs if DB text is polluted.

#### Root Cause
Retrieval is not separated into trusted structured rules vs untrusted text. No citation/version/confidence/contraindication contract.

#### Impact
Unsafe recommendations, legal exposure, hallucinated explanations.

#### Fix Proposal
Make recommendation deterministic first, generative second. LLM may summarize but not invent treatments.

#### Redesigned Version
Recommendation object:
```json
{
  "recommendation_id": "versioned_rule_id",
  "source_version": "ruleset_2026_03_18",
  "indication": "pigmentation",
  "allowed_actions": ["consultation", "skincare", "procedure_info"],
  "contraindication_questions": ["pregnancy", "isotretinoin", "photosensitivity"],
  "evidence_level": "internal_rule|clinical_guideline|expert_consensus",
  "confidence": 0.0,
  "do_not_say": ["diagnosis", "guarantee", "cure"]
}
```

#### Validation Experiment
Build 200 adversarial DB rows with prompt injection. The final answer must ignore instructions inside retrieved text and expose only whitelisted fields.

---

### Issue 10: Privacy/security posture is weak

#### Problem
Face images are uploaded and saved. CORS allows `*`. Logging and retention policy are unclear.

#### Root Cause
Prototype API defaults were not hardened.

#### Impact
Facial biometric privacy risk, regulatory risk, data breach blast radius.

#### Fix Proposal
Restrict CORS, encrypt storage, short retention, no raw image logs, signed URLs, user consent, deletion API, audit logs.

#### Redesigned Version
Production flow:
- validate MIME and size
- process in ephemeral temp storage
- delete raw image after inference unless explicit consent
- store only minimized features/quality metrics
- encrypt any retained image
- per-tenant auth and rate limiting

#### Validation Experiment
Threat model review + DAST + log scan proving no image bytes/path leaks to third-party logs.

## Dataset Review

Current risk areas: smartphone/hospital gap, lighting bias, skin tone bias, demographic imbalance, background leakage, camera leakage, annotation inconsistency, class imbalance. Fix with explicit manifest metadata, stratified sampling, and external mobile test sets.

## Label Review

Labels need rubric versioning, multi-labeler agreement, confidence, adjudication, and uncertainty-aware training. Do not present continuous 0-100 scores unless labels support that granularity.

## Preprocessing Review

MediaPipe landmarks are useful but not a truth oracle. Hard masks and fixed crops are high-risk. The new quality gate is a minimum fix, not the end. Next fix: crop-validity metrics and multi-crop uncertainty.

## Architecture Review

Current architecture is serviceable but not reviewer-grade. Better: shared image encoder + region tokens + ordinal heads + calibration + OOD/reject head. VLM should be used for quality/explanation/context, not as a free-form diagnosis engine.

## Training Review

Add subject-disjoint splits, robust augmentations, class-balanced/ordinal losses, calibration, and per-domain validation. Freeze/fine-tune schedule and learning-rate policy should be documented per experiment.

## Evaluation Review

Missing: external test, subgroup fairness, calibration, confidence intervals, repeated-capture stability, coverage/accuracy rejection curves, and baseline comparisons.

## RAG Review

Use structured retrieval with versioned rules. Vector search only as fallback with a similarity threshold and human-curated whitelist. LLM must never output retrieved free text that contains instructions.

## Inference Review

Use quality gate, OOD detection, uncertainty, repeated-capture aggregation, and safe rejection. Provide user instructions for retake instead of fake scores.

## Product Review

The product assumption “one smartphone image -> precise skin diagnosis -> treatment recommendation” is too aggressive. Safer product: “image quality checked skin-condition screening and consultation preparation,” with retake guidance and non-diagnostic language.

## Privacy Review

Faces are biometric data. Treat every upload as sensitive. Minimize retention. Restrict CORS. Add auth, rate limits, consent, deletion, and audit trails.

## Red Team Findings

Attack cases:
- blurred selfie gives confident scores
- printed face/photo replay
- makeup conceals pigmentation/wrinkles
- colored lighting changes pigment score
- glasses/hair occlude eye wrinkles
- extreme yaw passes face detector but invalidates ROI
- background or hospital marker leaks label
- DB row contains prompt injection: “ignore previous instructions”
- vector poisoning creates high-similarity unsafe procedure
- user asks for diagnosis/cure guarantee

Controls:
- quality gate and liveness/replay checks
- occlusion detector
- background leakage tests
- prompt-injection sanitizer and field whitelist
- recommendation contraindication guard
- medical-disclaimer and escalation policy

## Missing Experiments

1. Subject/device/site/time-disjoint split evaluation
2. Smartphone external validation
3. Skin tone subgroup analysis
4. Hard-mask vs soft-mask vs raw ablation
5. Background-only shortcut test
6. Landmark jitter sensitivity test
7. Repeated-capture stability test
8. Low-light/blur/JPEG corruption benchmark
9. Calibration per subgroup
10. RAG injection and retrieval poisoning test
11. Recommendation clinician review
12. Latency/load test for concurrent TFLite interpreters

## Missing Baselines

- Dermatologist-only inter-rater baseline
- Simple color/texture radiomics baseline
- ResNet/EfficientNet/ConvNeXt baseline
- Self-supervised in-domain encoder + linear head
- Multi-task vs separate-task model
- No-mask vs hard-mask vs soft-mask
- Rule-only recommendation vs RAG-enhanced recommendation

## Improved Pipeline

1. Upload/API gate: MIME, size, auth, rate limit, consent
2. Image QC: resolution, exposure, blur, face size, pose, occlusion
3. Face preprocessing: alignment, canonical mesh, soft masks, ROI validity
4. Model inference: multi-crop aggregation, task heads, uncertainty
5. Calibration/OOD: coverage-vs-error policy
6. Score normalization: demographic/device calibrated, no fake precision
7. Recommendation retrieval: deterministic rules first, vector fallback second
8. Safety guard: contraindications, medical wording filter, citations/version
9. Explanation: cite measured regions, quality caveats, uncertainty
10. Logging: no raw image retention by default, audit-only metadata

## Production Readiness Assessment

- Novelty: MEDIUM. Useful integration, not novel enough without robust VLM/multimodal evidence.
- Technical Quality: MEDIUM-LOW. Modular but fragile preprocessing and weak validation.
- Experimental Quality: LOW. Missing external/fairness/calibration/stability proof.
- Practical Value: MEDIUM. Good product potential if reframed as screening/consult-prep.
- Production Readiness: LOW before QA gates; MEDIUM after quality gate plus privacy/RAG controls.

Severity: CRITICAL
