<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# AI

## Purpose

LangGraph-based beauty consultation system that chains a facial skin scoring pipeline (TFLite / MediaPipe) with a ReAct LLM agent. The agent reads raw 0-100 skin scores, queries a Neo4j AuraDB of treatment rules, and optionally retrieves PubMed evidence via RAG to produce deterministic treatment recommendations for each skin region.

`main.py` is a **CLI chat entrypoint only** — it instantiates `beauty-agent/agent/graph.py:ChatSession` and streams tokens to stdout. There is no FastAPI server in this directory; all HTTP exposure is handled by the Spring Boot backend.

## Key Files

| File | Description |
|---|---|
| `main.py` | CLI chat entrypoint. Adds `beauty-agent/` to `sys.path`, creates a `ChatSession`, and runs an interactive token-streaming REPL. Flags: `--help`. Exit on `exit`/`quit`/`Ctrl-C`. |
| `README.md` | Full architecture diagram (LangGraph think→act→observe→finish cycle), data-flow ASCII art, AuraDB schema, pipeline output format, Docker quick-start, and troubleshooting table. |
| `.env.example` | Template for the single shared `.env` consumed by all submodules. Copy to `.env` and fill in real values before running. |
| `docker-compose.yml` | Recommended one-command runner. Service `beauty-agent` builds from `./docker`, mounts `.:/workspace`, injects `.env`, and defaults to `python main.py`. |
| `docker_build.sh` | Legacy: `docker build -t skin_inference:latest ./docker` |
| `docker_run.sh` | Legacy: starts container `skin_inference` with bind-mount. |
| `docker_attach.sh` | Legacy: exec-shell into running `skin_inference` container. |
| `inference.sh` | Standalone pipeline smoke-test: `python pipeline/pipeline.py --image <sample>` — use to verify TFLite models load correctly. |

## Subdirectories

| Directory | Purpose |
|---|---|
| `beauty-agent/` | LangGraph ReAct agent (graph, state, prompts, tools). See `beauty-agent/AGENTS.md`. |
| `pipeline/` | `SkinPipeline` class — runs MediaPipe face landmark cropping then EfficientNet TFLite inference for age/pigment/wrinkle/homogenity/sagging. See `pipeline/AGENTS.md`. |
| `classification/` | Offline ML training and conversion code that produced the TFLite models consumed by `pipeline/`. See `classification/AGENTS.md`. |
| `auradb/` | Neo4j AuraDB upload scripts, connection helpers, and deterministic rule-matcher. See `auradb/AGENTS.md`. |
| `docker/` | `Dockerfile` (`python:3.11-slim`; installs tensorflow-cpu, mediapipe, langchain, langgraph, neo4j, openpyxl; runs as user1 UID 1000 in `/workspace`). |
| `samples/` | Test face images used by `inference.sh` and chat examples (`028_data/`, `034_data/`, `sample_crop/`). |

## For AI Agents

### Working In This Directory

- All commands run from the repo root (`AI/`). The working directory inside Docker is `/workspace` which maps to this directory.
- `.env` must exist with real keys before running anything. Copy from `.env.example`.
- TFLite model files (`pipeline/inference_models/*.tflite`, `face_landmarker.task`) are not in Git — confirm they exist before running `inference.sh` or the agent.
- Prefer `docker compose run --rm beauty-agent` over the legacy shell scripts for new workflows.
- To reload AuraDB rules after editing the Excel: `docker compose run --rm beauty-agent python auradb/aura_upload_beauty_rules.py` (wipes and re-ingests; costs ~$0.001 in OpenAI embeddings).

### Testing Requirements

- No automated test suite exists. Validation is manual:
  1. Run `bash inference.sh` inside the container to confirm pipeline output format.
  2. Run the chat session and issue a prompt with an image path and gender to exercise the full agent loop.
- Do not run `train.py` or any `classification/` code in CI — training requires GPU and wandb credentials.

### Common Patterns

- `beauty-agent/config.py` holds the LLM model ID (`AI_MODEL`, e.g. `openai:gpt-4o-mini`) — change it there, not in `.env`.
- The agent enforces one tool call per LLM turn (see `agent/prompts.py`). If the model emits a raw JSON block instead of a function call, switch models or adjust `max_tokens`.
- `skin_analyze` caches `SkinPipeline` by gender key to avoid repeated TFLite load cost.
- `InMemorySaver` + `thread_id` provides multi-turn state persistence within a single process; state is lost on restart.

## Dependencies

### Internal

- `beauty-agent/` → `pipeline/SkinPipeline`, `auradb/rule_matcher`, `auradb/Connect_DB`
- `pipeline/` uses TFLite models produced by `classification/` (copy manually to `pipeline/inference_models/`)

### External

| Package | Use |
|---|---|
| `langchain` / `langgraph` | ReAct StateGraph, ToolNode, InMemorySaver |
| `openai` | LLM calls (chat completions) + text-embedding-3-small |
| `google-generativeai` | Optional alternative LLM backend |
| `neo4j` | AuraDB driver |
| `tensorflow-cpu` | TFLite runtime in pipeline |
| `mediapipe` | Face landmark detection in pipeline |
| `openpyxl` | Reading `auradb/data/mapping_table_tmp.xlsx` |
| `requests` | PubMed E-utilities (esearch, efetch) |

## Environment Variables

All variables are loaded from the single `.env` file at the repo root.

| Variable | Required | Description |
|---|---|---|
| `OPENAI_API_KEY` | Yes | OpenAI chat completions and embeddings |
| `GOOGLE_API_KEY` | No | Gemini model alternative |
| `PUBMED_API_KEY` | No | Raises PubMed rate limit from 3 to 10 req/s |
| `NEO4J_URI` | Yes | AuraDB connection string (`neo4j+s://...`) |
| `NEO4J_USERNAME` | Yes | AuraDB username (default: `neo4j`) |
| `NEO4J_PASSWORD` | Yes | AuraDB password |
| `NEO4J_DATABASE` | No | Leave blank for default AuraDB database |
| `AURA_API_CLIENT_ID` | No | Aura Agent API (optional feature) |
| `AURA_API_CLIENT_SECRET` | No | Aura Agent API (optional feature) |
| `AGENT_ID` | No | Aura Agent API (optional feature) |
| `OPENAI_EMBED_MODEL` | No | Embedding model (default: `text-embedding-3-small`) |
| `OPENAI_EMBED_DIMENSIONS` | No | Embedding dimensions (default: `1536`) |
| `EMBED_BATCH_SIZE` | No | Embedding batch size (default: `16`) |
| `EMBED_MIN_BATCH_SIZE` | No | Min batch size for retry (default: `4`) |
| `EMBED_MAX_RETRIES` | No | Max embedding retries (default: `8`) |
| `EMBED_RETRY_BASE_SECONDS` | No | Retry base delay in seconds (default: `1.0`) |
| `EXCEL_PATH` | No | Path to treatment rules Excel (default: `auradb/data/mapping_table_tmp.xlsx`) |
| `LANGCHAIN_API_KEY` | No | LangSmith tracing key |
| `LANGCHAIN_TRACING_V2` | No | Enable LangSmith tracing (`true`/`false`, default: `false`) |
| `LANGCHAIN_PROJECT` | No | LangSmith project name (default: `beauty-agent`) |
| `LANGCHAIN_ENDPOINT` | No | LangSmith endpoint URL |

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
