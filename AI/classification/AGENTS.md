<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# classification

## Purpose

Offline ML training, evaluation, and TFLite conversion package for the skin-analysis models consumed by `pipeline/`. Produces `.tflite` files from EfficientNetV2 Keras models trained on facial crop datasets. This package is **not imported at runtime** — its outputs (`.tflite` files) are copied to `pipeline/inference_models/` and loaded there by `SkinPipeline`.

Framework: TensorFlow / Keras (EfficientNetV2 backbone family). Experiment tracking: Weights & Biases (wandb). Face preprocessing: MediaPipe. Notifications: Slack webhook.

## Key Files

| File | Description |
|---|---|
| `train.py` | Main training entrypoint. Selects task (age_cls, wrinkle_cls, wrinkle_reg, pigment_cls, pigment_reg, homogenity_cls), builds EfficientNetV2 model, loads YAML config from `configs/`, initializes wandb run, calls the matching `tasks/<task>.train()`. Seeds TF/NumPy/Python at 42. GPU memory growth enabled. Args: `--config <yaml>`, `--test` (disables wandb). |
| `inference.py` | Batch inference script for evaluating trained `.keras` models against a JSON test split. Loads models whose epoch number is a multiple of 10, runs EfficientNetV2 `preprocess_input`, writes CSV results to a timestamped output directory. Paths are hard-coded for the training server — edit before running. |
| `setup.py` | Minimal `setuptools` package registration (`name=hobbang`, `version=v0.2`). Allows `find_packages()` to discover `models/`, `dataloaders/`, `tasks/`, etc. as importable packages. |
| `age_viz.py` | Age classification visualization utilities. |
| `confusion_matrix.py` | Generates confusion matrices from inference results. |
| `make_tflite_models.py` | Batch conversion of `.keras` checkpoints to `.tflite` using `convert/keras2tflite.py`. |
| `face_landmarker.task` | MediaPipe FaceLandmarker model file used during preprocessing. |
| `run_confusion.sh` | Shell script to invoke `confusion_matrix.py`. |

## Subdirectories

### models/

EfficientNetV2 model-building functions. Each task module exposes a `build_model()` function that returns `(model, backbone_name)`. The backbone is selected from `MODEL_DICT` in `model_params.py` (B0–B3, S, M, L variants). All models use ImageNet pretrained weights and full fine-tuning.

| Module | Description |
|---|---|
| `age_cls.py` | Shared EfficientNetV2 backbone + gender-split dual heads (male/female Dense → softmax). Input: single face image (384×384×3). Output: 7-class probability per gender head (decade 10s–70s). |
| `pigment_cls.py` | Pigmentation classification model. |
| `pigment_reg.py` | Pigmentation regression model (continuous 0–100 score). |
| `wrinkle_cls.py` | Wrinkle classification model. |
| `wrinkle_reg.py` | Wrinkle regression model. |
| `homogenity_cls.py` | Skin homogeneity (radiance/texture) classification model. |
| `common_layers.py` | Shared custom Keras layers (e.g., `VChannelEqualizer` for HSV V-channel normalization — currently commented out in most models but available). |
| `model_params.py` | `MODEL_DICT`: maps string keys (`'B0'`..`'L'`) to `tensorflow.keras.applications.EfficientNetV2*` classes. |

### dataloaders/

TensorFlow `tf.data.Dataset` builders. Each module exposes `get_datasets(config)` returning `(train_dataset, val_dataset)`. Reads JSON split metadata files, decodes JPEG images, resizes, normalizes to 0–1, and applies augmentation from `base_dataloader.py:Augmentation`.

| Module | Description |
|---|---|
| `base_dataloader.py` | `Augmentation` class with static methods: flip, Gaussian noise, Poisson noise, HSV brightness/contrast/gamma correction, histogram equalization (with optional MediaPipe face-oval mask). All augmentations operate on 0–1 float tensors using `tf.cond` for stochastic application. |
| `age_cls.py` | Age classification dataloader. |
| `pigment_cls.py` | Pigmentation classification dataloader. |
| `pigment_reg.py` | Pigmentation regression dataloader. |
| `wrinkle_cls.py` | Wrinkle classification dataloader. |
| `wrinkle_reg.py` | Wrinkle regression dataloader. |
| `homogenity_cls.py` | Homogeneity classification dataloader. |

### tasks/

Training loop implementations. Each module exposes `train(train_dataset, val_dataset, config)`. Compiles the model with Adam optimizer, plugs in wandb metrics logger, custom callbacks (`SaveEveryKEpochs`, task-specific eval callback), and `ReduceLROnPlateau`.

| Module | Description |
|---|---|
| `age_cls.py` | Dual-head loss dict (`{male: loss, female: loss}`), AUC metric per head. Saves `.keras` checkpoint every 10 epochs. |
| `pigment_cls.py` | Pigmentation classification training loop. |
| `pigment_reg.py` | Pigmentation regression training loop (uses `RangeMSELoss`). |
| `wrinkle_cls.py` | Wrinkle classification training loop. |
| `wrinkle_reg.py` | Wrinkle regression training loop (uses `RangeMSELoss`). |
| `homogenity_cls.py` | Homogeneity classification training loop. |

### preprocess/

Data preparation scripts. Run once before training; not imported at inference time.

| File | Description |
|---|---|
| `crop_data.py` | Crops raw face images to per-region patches using MediaPipe landmarks. |
| `extract_EXIF.py` | Reads EXIF metadata from images (e.g., orientation). |
| `face_crop_roll.py` | Handles roll-angle face alignment before cropping. |
| `hist_extractor.py` | Extracts histogram features from image patches. |
| `remove_non_skin.py` | Filters images where the face region is not clearly skin (quality gating). |

Also contains versioned Jupyter notebooks (`092_preprocess.ipynb`, `histogram_equalization_test.ipynb`, `new_data_dir.ipynb`, `vram_abnormal.ipynb`) and a helper shell script (`check_number_of_files.sh`).

### custom_losses/

| File | Description |
|---|---|
| `range_mse_loss.py` | `RangeMSELoss(tf.keras.losses.Loss)`: penalizes predictions that fall outside the GT label's score range. Used by pigment_reg and wrinkle_reg tasks. Ranges defined in `utils/constants.py:PIGMENT_RANGES`. |

### utils/

Shared utilities for training support.

| Module | Description |
|---|---|
| `constants.py` | `SECTORS` (7 wrinkle regions: forehead, right_eye, left_eye, nasolabial, perioral, right_vol, left_vol), `SECTOR2IDX`/`IDX2SECTOR` dicts, `PIGMENT_RANGES` (5-class boundary table for RangeMSELoss), `WEBHOOK_URL` (Slack, loaded from `no_track/webhook_url.txt`). |
| `callbacks.py` | Custom Keras callbacks: `SaveTopKModels`, `SaveEveryKEpochs`, `ClearMemoryCallback`, `AgeClsCallBack`. |
| `histogram_equalizer.py` | Standalone histogram equalization utility. |
| `Mediapipe.py` | MediaPipe FaceLandmarker wrapper for preprocessing. |
| `metrics.py` | Custom Keras metrics (e.g., range-aware accuracy for regression tasks). |
| `slack.py` | `send_slack_message()`: posts training start/finish/error notifications to a Slack webhook. |
| `tensorflow.py` | TF session/device configuration helpers. |
| `wandb.py` | `load_wandb_model()`: downloads a Keras model artifact from a wandb run for use in conversion. |

### convert/

Model export utilities.

| File | Description |
|---|---|
| `keras2tflite.py` | Converts a `.keras` checkpoint to a `.tflite` file via `tf.lite.TFLiteConverter`. Handles multi-input shapes (age: 3×(384,384,3); pigment: (384,384,6); others: (384,384,3)). Loads model path from wandb artifact or direct path. |
| `keras2trt.py` | TensorRT engine conversion for GPU deployment (separate inference path, not used by `pipeline/`). |
| `make_trt.sh` | Shell script to invoke `keras2trt.py`. |

### inference/

Per-model inference scripts for evaluating trained checkpoints. Three backends:

| Subdirectory | Description |
|---|---|
| `inference/Origin/` | Native Keras inference for age_cls, pigment_cls, wrinkle_cls. Each has `one.py` (single image), `batch.py` (directory), `GradCAM.py` (class activation maps), and visualization scripts. |
| `inference/TFLITE/` | TFLite delegate inference scripts. Includes `legacy_pipeline.py` (early version of `pipeline/`), and per-task scripts with ensemble support for age. |
| `inference/TensorRT/` | TensorRT inference scripts for age_cls, pigment_cls, wrinkle_cls (GPU server only). |

### configs/

YAML training configuration files, one per task variant. Each file specifies: `task`, `model_size`, `input_shape`, `epochs`, `batch_size`, `learning_rate`, `loss`, `kernel_regularizer`, `meta_file` (path to JSON split), `root_dir`, `save_dir`.

| File | Task |
|---|---|
| `age_cls.yaml` | Age decade classification |
| `homogenity_cls.yaml` | Skin homogeneity classification |
| `pigment_cls.yaml` | Pigmentation classification |
| `pigment_reg.yaml` | Pigmentation regression |
| `wrinkle_cls.yaml` | Wrinkle classification |
| `wrinkle_reg.yaml` | Wrinkle regression |

## For AI Agents

### Working In This Directory

- **Do not run `train.py` in any automated or CI context.** Training requires a CUDA GPU (tested on 20 GB VRAM), wandb API key from `no_track/wandb_key.txt`, and training data paths that are server-specific.
- `inference.py` also has hard-coded server paths — edit before running.
- The package is installed editably in the training environment (`python setup.py develop` or `pip install -e .`). In the Docker runtime (`docker/`) this package is **not installed** — only `pipeline/` is used there.
- The Slack webhook URL is loaded from `no_track/webhook_url.txt` at import time; `WEBHOOK_URL` silently becomes `None` if the file is missing (safe to ignore in non-training environments).
- Augmentation and data loading rely on `tf.data` pipelines; all tensor ops use `tf.cond` for stochastic branching (not Python conditionals) to stay graph-compatible.

### Testing Requirements

- No automated tests exist. Manual validation workflow:
  1. Train a model with `python train.py --config <task>.yaml`
  2. Evaluate with `inference.py` (edit paths) or a script in `inference/Origin/`
  3. Review confusion matrix via `confusion_matrix.py`
  4. Convert with `make_tflite_models.py` or `convert/keras2tflite.py`
  5. Copy resulting `.tflite` to `pipeline/inference_models/` and validate via `inference.sh` at the repo root

### Common Patterns

- All models use `build_model()` as the single constructor entry point, returning `(tf.keras.Model, str)` (model, backbone_name).
- Task dispatch in `train.py` is a plain `if/elif` chain keyed on `config['task']` string — add a new task by adding a new branch and corresponding `models/`, `dataloaders/`, `tasks/` modules.
- Augmentation is composed by passing a list of string names (`aug_list`) to `Augmentation.apply_aug()`. HSV-space augmentations are batched into a single RGB→HSV→RGB round-trip for efficiency.
- `RangeMSELoss` penalizes predictions outside the GT class range rather than penalizing distance to a single target point — preserves score-range semantics for the 0-100 output scale.
- wandb config is the single source of truth inside `train()` after `wandb.init()` — access via `config.<key>` (wandb config proxy), not the raw dict.

## Dependencies

### Internal

- `pipeline/inference_models/` receives the `.tflite` outputs of `convert/keras2tflite.py`
- `utils/constants.py:PIGMENT_RANGES` is shared by `custom_losses/range_mse_loss.py` and `utils/metrics.py`

### External

| Package | Use |
|---|---|
| `tensorflow` (GPU build) | Model training, `tf.data` pipelines, TFLite conversion |
| `wandb` | Experiment tracking, model artifact storage/retrieval |
| `mediapipe` | FaceLandmarker for preprocessing and augmentation masking |
| `numpy` | Array ops in dataloaders and evaluation |
| `pyyaml` | YAML config loading in `train.py` |
| `requests` | Slack webhook in `utils/slack.py` |
| `tqdm` | Progress bars in `inference.py` and batch scripts |
| `setuptools` | Package discovery via `setup.py` |

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
