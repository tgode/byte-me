# ByteHR AI — Model Benchmark

**Date:** 2026-06-18  
**Purpose:** Performance comparison of Qwen3 models for demo mode

---

## Summary

| Model | Disk | RAM | Simple prompt | HR prompt (1500-char ctx) | Demo suitability |
|---|---|---|---|---|---|
| `qwen3:8b` | ~4.9 GB | ~6 GB | 30.0s | ~98.9s | ❌ Exceeds 60s limit |
| `qwen3:4b` | ~2.3 GB | ~3 GB | ~15s | ~50s | ⚠️ Borderline |
| **`qwen3:1.7b`** | **~1.1 GB** | **~2 GB** | **8.3s** | **16.7s** | **✅ Demo-ready** |

---

## Measurements (Ollama v0.30.10, CPU inference, think=false)

### qwen3:8b (previous default)

| Scenario | total_duration |
|---|---|
| Simple: `"Reply with exactly: OK"` | 30.0s |
| HR question + 1500-char context | ~98.9s |

### qwen3:1.7b (current default)

| Scenario | total_duration | eval_count (tokens) |
|---|---|---|
| Simple: `"Reply with exactly: OK"` | **8.34s** | 110 |
| HR question + 1500-char context | **16.71s** | 198 |

**Speedup vs qwen3:8b:** ~6× faster on simple prompts, ~6× faster on HR questions.

---

## Configuration — Current Default

```yaml
# application.yml
ollama:
  chat-model: ${OLLAMA_CHAT_MODEL:qwen3:1.7b}
  embedding-model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
```

```env
# .env.example
OLLAMA_CHAT_MODEL=qwen3:1.7b
```

---

## Startup Log Output

On every application startup:

```
[Model Config]
[Model Config] Chat Model     : qwen3:1.7b
[Model Config] Embedding Model: nomic-embed-text
```

---

## Per-Request Performance Log

Emitted at `INFO` level before and after every LLM generation call:

```
[Chat] model=qwen3:1.7b
[Chat] promptChars=1450
[Chat] estimatedTokens=362
[Chat] generationDurationMs=16710
```

| Field | Source | Notes |
|---|---|---|
| `model` | `ollama.chat-model` config | Model actually used for generation |
| `promptChars` | Sum of all message content lengths | System + history + question |
| `estimatedTokens` | `promptChars / 4` | Approximation (4 chars/token for EN/SQ/SR) |
| `generationDurationMs` | Wall-clock elapsed in `OllamaClient` | Includes network + model inference |

---

## Switching Models

### Switch to qwen3:3b / qwen3:4b / qwen3:8b

```bash
# .env — update this value and restart the API container
OLLAMA_CHAT_MODEL=qwen3:4b    # or qwen3:8b for higher quality

# Pull the model first
ollama pull qwen3:4b

# Restart only the API (no DB/Ollama restart needed)
docker compose up -d --no-deps bytehr-api

# Or for local development without Docker
# Set BYTEHR_SOURCE_TYPE=local and OLLAMA_CHAT_MODEL=qwen3:4b
```

### Quick model comparison commands

```bash
# Benchmark any model in ~60 seconds
MODEL=qwen3:1.7b  # change to test others

# Simple latency
time curl -s http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"Reply: OK\"}],\"stream\":false,\"options\":{\"think\":false}}" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'{d[\"total_duration\"]/1e9:.1f}s')"

# HR-context latency
time curl -s http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"system\",\"content\":\"Answer HR questions. Policy: 20 vacation days per year per Albanian Labour Code.\"},{\"role\":\"user\",\"content\":\"How many vacation days do I get?\"}],\"stream\":false,\"options\":{\"think\":false,\"num_ctx\":4096}}" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(repr(d.get('message',{}).get('content','')[:80])); print(f'{d[\"total_duration\"]/1e9:.1f}s')"
```

---

## Resource Requirements

| Model | Disk | Min RAM | Notes |
|---|---|---|---|
| `nomic-embed-text` | ~270 MB | ~1 GB | Embeddings only; must always be present |
| `qwen3:1.7b` | ~1.1 GB | ~2 GB | **Current demo default** |
| `qwen3:4b` | ~2.3 GB | ~3 GB | Better quality, ~3× slower |
| `qwen3:8b` | ~4.9 GB | ~6 GB | Production quality, ~6× slower |

**Total for demo stack:** `nomic-embed-text` + `qwen3:1.7b` = ~1.4 GB models, ~3 GB RAM total.

---

## Model Pull Commands

```bash
# Required for demo
ollama pull nomic-embed-text
ollama pull qwen3:1.7b

# Optional — alternative chat models
ollama pull qwen3:4b
ollama pull qwen3:8b

# Verify installed
ollama list
```

---

## Quality vs Speed Trade-off

`qwen3:1.7b` is sufficient for the hackathon demo because:

- HR documents are factual and structured (less ambiguity for the LLM to resolve)
- The RAG pipeline constrains the answer to retrieved document context
- The system prompt enforces concise, citation-backed responses
- Multilingual support (Albanian, Serbian, English) is built into the qwen3 family

For production with higher query volume and quality requirements, use `qwen3:8b` or larger.
