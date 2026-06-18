# ByteHR AI — Performance Optimization for Demo Mode

**Date:** 2026-06-18  
**Objective:** Reduce LLM response latency from 90–150s to <60s for hackathon demo

---

## Baseline Measurements (Before Optimization)

| Scenario | Model | think | topK | contextChars | Measured |
|---|---|---|---|---|---|
| Simple prompt ("OK") | qwen3:8b | on | — | — | **54.9s** |
| Simple prompt ("OK") | qwen3:8b | off | — | — | **30.0s** |
| HR question, full context | qwen3:8b | on | 5 | ~5000 | **90–150s** (est.) |
| HR question, 1500-char ctx | qwen3:8b | off | 3 | 1500 | **98.9s** |

### Root Causes of Latency

1. **qwen3:8b thinking mode (chain-of-thought)** — adds 20–30s of reasoning tokens before the answer
2. **Unconstrained context size** — 5 chunks × 1000 chars = up to 5000+ context chars → large prompt
3. **Single 60s timeout** — too short for all scenarios; caused timeouts before any answer was returned
4. **topK=5** — extra retrieved chunks add context chars, embedding time, and SQL query cost

---

## Optimizations Implemented

### 1. topK: 5 → 3

**Config:** `bytehr.rag.top-k=3`  
**Impact:** 40% fewer chunks retrieved, 40% less SQL work, 40% less context fed to LLM

```yaml
# application.yml
bytehr:
  rag:
    top-k: ${BYTEHR_RAG_TOP_K:3}      # was: vector-search.top-k=5
```

### 2. Context size limit: unlimited → 1500 chars

**Config:** `bytehr.rag.max-context-chars=1500`  
**Impact:** Caps the number of tokens in the system prompt, directly proportional to LLM generation time

```yaml
bytehr:
  rag:
    max-context-chars: ${BYTEHR_RAG_MAX_CONTEXT_CHARS:1500}  # was: no limit
```

**Chunk truncation logic** (in `HrResponseAgentImpl.buildContext()`):
- Chunks are added in relevance order until the character budget is exhausted
- A chunk that would overflow is truncated at the character boundary with `...`
- A chunk whose header alone exceeds the budget is omitted entirely

### 3. LLM thinking mode disabled: `think: false`

**Code:** `OllamaChatOptions.think = false`  
**Impact:** Suppresses chain-of-thought reasoning tokens in qwen3

```java
OllamaChatOptions.builder()
    .temperature(0.1)
    .numCtx(4096)
    .think(false)   // ← disables CoT; reduces latency by ~45%
    .build()
```

| | think=true | think=false |
|---|---|---|
| Simple prompt | 54.9s | 30.0s |
| Improvement | — | **-45%** |

### 4. Model switch: `qwen3:8b` → `qwen3:4b`

**Config:** `OLLAMA_CHAT_MODEL=qwen3:4b`

| | qwen3:8b | qwen3:4b |
|---|---|---|
| Parameters | 8.2B | 4.0B |
| Disk size | ~4.9 GB | ~2.3 GB |
| RAM required | ~6 GB | ~3 GB |
| CPU inference speed | baseline | **~2× faster** |
| Expected latency (HR prompt) | ~100s | **~45–55s** |

> **Pull before demo:**
> ```bash
> ollama pull qwen3:4b
> ```
> Or via Docker Compose (automatic on first startup via `ollama-init`).

### 5. Separate timeouts per operation

| Operation | Before | After |
|---|---|---|
| Embedding (`nomic-embed-text`) | 60s | **120s** |
| Chat (`qwen3:4b`) | 60s | **300s** |

### 6. Prompt token logging

Added before every LLM call:

```
[RAG] Prompt stats: messages=3, totalChars=1847, estimatedTokens=~461, contextChars=1498, historyTurns=0
```

Estimation: `totalChars / 4` (4 chars/token approximation for English/Albanian/Serbian).

---

## New Configuration Properties

| Property | Default | Env var | Description |
|---|---|---|---|
| `bytehr.rag.top-k` | `3` | `BYTEHR_RAG_TOP_K` | Chunks retrieved per query |
| `bytehr.rag.max-context-chars` | `1500` | `BYTEHR_RAG_MAX_CONTEXT_CHARS` | Max document context chars sent to LLM |
| `ollama.embedding-timeout-seconds` | `120` | `OLLAMA_EMBEDDING_TIMEOUT` | Timeout for embedding calls |
| `ollama.chat-timeout-seconds` | `300` | `OLLAMA_CHAT_TIMEOUT` | Timeout for LLM generation |

---

## After Optimization — Expected Performance

| Scenario | Model | think | topK | contextChars | Expected |
|---|---|---|---|---|---|
| HR question | qwen3:4b | off | 3 | ≤1500 | **~45–55s** |
| HR question | qwen3:8b | off | 3 | ≤1500 | **~95–100s** |
| HR question | qwen3:8b | on | 5 | ~5000 | **90–150s** (no timeout) |

> **Note:** LLM generation times on CPU are hardware-dependent. Results measured on a consumer-grade Windows PC (Intel/AMD CPU). GPU acceleration would reduce times by 10–20×.

---

## Demo Mode vs Production Settings

| Setting | Demo (default) | Production |
|---|---|---|
| `bytehr.rag.top-k` | 3 | 5–10 |
| `bytehr.rag.max-context-chars` | 1500 | 3000–6000 |
| `ollama.chat-model` | `qwen3:4b` | `qwen3:8b` or larger |
| `think` | false | false (or true for complex queries) |
| `chat-timeout-seconds` | 300 | 600 |

Switch to production settings via `.env`:

```env
OLLAMA_CHAT_MODEL=qwen3:8b
BYTEHR_RAG_TOP_K=5
BYTEHR_RAG_MAX_CONTEXT_CHARS=3000
OLLAMA_CHAT_TIMEOUT=600
```

---

## Files Changed

| File | Change |
|---|---|
| `config/RagProperties.java` | New `@ConfigurationProperties(bytehr.rag)` class |
| `config/AppConfig.java` | Register `RagProperties` |
| `service/impl/HrResponseAgentImpl.java` | Use `RagProperties`; context truncation; prompt stats logging |
| `integration/ollama/dto/OllamaChatOptions.java` | Add `think: false` field |
| `application.yml` | Add `bytehr.rag.*`; model → `qwen3:4b`; DEBUG for HrResponseAgentImpl |
| `.env.example` | Update model + RAG env vars |
| `docker-compose.yml` | Update model + RAG env vars in bytehr-api service |
| `scripts/init-ollama.sh` | Pull `qwen3:4b` instead of `qwen3:8b` |

---

## Demo Preparation Checklist

```bash
# 1. Pull qwen3:4b (if not already installed)
ollama pull qwen3:4b

# 2. Verify models
ollama list
# Should show: nomic-embed-text, qwen3:4b

# 3. Start stack
docker compose up -d

# 4. Index documents
curl -X POST http://localhost:8080/api/sync
# Expected: documentsProcessed=12, totalDocumentsIndexed=12

# 5. Warm up the model (first call loads it into RAM)
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"Hello"}'

# 6. Demo question
time curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"How many vacation days do I have?","country":"AL"}'
# Expected: answered=true, confidence>0, citations present, <60s
```
