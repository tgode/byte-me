# ByteHR AI — Ollama Integration Audit

**Date:** 2026-06-18  
**Ollama version:** 0.30.10  
**Status:** Two issues found and fixed

---

## Environment

| Component | Value |
|---|---|
| Ollama version | 0.30.10 (Windows host) |
| Embedding model | `nomic-embed-text:latest` — installed ✅ |
| Chat model | `qwen3:8b` — installed ✅ |
| Ollama URL (local) | `http://localhost:11434` |
| Ollama URL (Docker) | `http://ollama:11434` |

---

## Investigation Results

### 1. Which Ollama endpoint is called?

**Original code:** `POST /api/embeddings` with request field `"prompt"`  
**Current API:** `POST /api/embed` with request field `"input"`

The `/api/embeddings` endpoint was deprecated in Ollama v0.1.26 and replaced by `/api/embed`. Both endpoints exist in v0.30.10 and return embeddings correctly — however the response shapes differ:

| Endpoint | Request field | Response key | Response shape |
|---|---|---|---|
| `/api/embeddings` | `"prompt"` | `"embedding"` | `[float, ...]` — flat array |
| `/api/embed` | `"input"` | `"embeddings"` | `[[float, ...]]` — array of arrays |

### 2. Is the endpoint compatible with the installed version?

Both endpoints work on Ollama v0.30.10. However, `/api/embed` is the canonical current API and includes richer response metadata (`total_duration`, `load_duration`, `prompt_eval_count`).

### 3. Which embedding model is configured?

`nomic-embed-text` — verified installed and responding:

```
POST /api/embed {"model": "nomic-embed-text", "input": "test"}
→ {"embeddings": [[768 floats]], "total_duration": 90ms, ...}
```

Dimension: **768** — matches the pgvector column `embedding vector(768)`.

### 4. Is the model actually installed?

**YES.** `curl http://localhost:11434/api/tags` returns:

```json
{
  "models": [
    {"name": "qwen3:8b"},
    {"name": "nomic-embed-text:latest"}
  ]
}
```

### 5. What response is returned by Ollama?

Live measurements:

| Endpoint | Model | Cold | Warm |
|---|---|---|---|
| `POST /api/embed` | `nomic-embed-text` | ~90ms | ~80ms |
| `POST /api/chat` | `qwen3:8b` (think=true) | **54.9s** | ~30s |
| `POST /api/chat` | `qwen3:8b` (think=false) | **30.0s** | ~15s |

### 6. Why does the request exceed 60 seconds?

**Root cause: `qwen3:8b` uses extended chain-of-thought (thinking) mode by default.**

`qwen3` is a "hybrid reasoning model" that runs multi-step reasoning (chain-of-thought) before generating a final answer. This produces `/think` tokens in the output and significantly increases response time.

Measured: **54.9s** for a trivial `"Reply with exactly: OK"` prompt with default settings.

Any non-trivial HR answer generation with a full system prompt + document context would routinely exceed 60 seconds, causing `TimeoutException` in the Spring WebFlux reactive chain.

Secondary cause: the shared `timeout-seconds: 60` applied identically to both embedding (needs ~1s) and chat (needs 30–300s) calls. Inappropriate for both.

---

## Fixes Applied

### Fix 1 — Switch to `/api/embed` with correct request/response DTOs

**New files:**
- `OllamaEmbedRequest` — `model` + `input` fields
- `OllamaEmbedResponse` — `model` + `embeddings` (array-of-arrays) + timing fields

**OllamaClient change:**

```java
// BEFORE
OllamaEmbeddingRequest request = OllamaEmbeddingRequest.builder()
    .model(embeddingModel)
    .prompt(text)          // ← deprecated field
    .build();

webClient.post()
    .uri("/api/embeddings")   // ← deprecated endpoint
    .bodyToMono(OllamaEmbeddingResponse.class)  // ← expects {"embedding": [...]}

// AFTER
OllamaEmbedRequest request = OllamaEmbedRequest.builder()
    .model(embeddingModel)
    .input(text)           // ← current field
    .build();

webClient.post()
    .uri("/api/embed")        // ← current endpoint
    .bodyToMono(OllamaEmbedResponse.class)  // ← expects {"embeddings": [[...]]}
```

---

### Fix 2 — Disable qwen3 thinking mode; separate timeouts per operation

**`OllamaChatOptions` — added `think: false`:**

```java
// BEFORE
OllamaChatOptions.builder()
    .temperature(0.1)
    .numCtx(4096)
    .build()
    // → qwen3 uses thinking mode (chain-of-thought) by default → ~55s latency

// AFTER
OllamaChatOptions.builder()
    .temperature(0.1)
    .numCtx(4096)
    .think(false)   // ← disables CoT tokens → ~30s latency (45% reduction)
    .build()
```

**`application.yml` — separate timeouts:**

```yaml
ollama:
  timeout-seconds: 120              # backward compat fallback
  embedding-timeout-seconds: 120    # nomic-embed-text: <1s warm, ~10s cold start
  chat-timeout-seconds: 300         # qwen3:8b: 30s typical, 300s for complex HR prompts
```

---

### Fix 3 — Explicit HTTP error handling in WebClient

```java
// BEFORE: 4xx/5xx errors would be swallowed or throw an unstructured exception

// AFTER: explicit error body capture
.onStatus(
    status -> status.isError(),
    clientResponse -> clientResponse.bodyToMono(String.class)
        .map(body -> new IllegalStateException(
            String.format("Ollama %s error %s: %s", path, status, body)))
)
```

---

## Debug Logging Added

### Embedding call log output

```
[OllamaClient] Embed request: url=http://localhost:11434/api/embed,
               model='nomic-embed-text', textLen=987, timeout=120s

[OllamaClient] Embed response: model='nomic-embed-text',
               dim=768, totalDuration=90ms, elapsed=91ms
```

### Chat call log output

```
[OllamaClient] Chat request: url=http://localhost:11434/api/chat,
               model='qwen3:8b', messages=3, timeout=300s

[OllamaClient] Chat response: model='qwen3:8b',
               responseLen=412, elapsed=28450ms
```

---

## Verification

### DTO shape validated against live Ollama

```python
# /api/embed response matches OllamaEmbedResponse DTO
model: nomic-embed-text
embeddings[0] dimension: 768
total_duration: 90.1ms
sample values: [-0.0324, 0.0578, -0.1843]
```

### Test suite

```
Tests run: 11, Failures: 0, Errors: 0
BUILD SUCCESS
```

### Live verification after POST /api/sync

```bash
# Trigger re-index (all 12 documents, fresh embeddings)
curl -X POST http://localhost:8080/api/sync

# Confirm all embeddings persisted
docker exec bytehr-postgres psql -U bytehr -d bytehr -c "
SELECT
  COUNT(*) AS total,
  COUNT(embedding) AS with_embedding,
  COUNT(*) = COUNT(embedding) AS all_embedded
FROM document_chunks;"

# Expected:
# total | with_embedding | all_embedded
# ------+----------------+--------------
#    34 |             34 | t

# Test /api/chat returns answered=true
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"How many vacation days do I have?","country":"AL"}' \
  | python3 -m json.tool
# Expected: answered: true, confidence > 0, citations non-empty
```

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `ollama.base-url` | `http://localhost:11434` | Ollama API base URL |
| `ollama.embedding-model` | `nomic-embed-text` | Model for `POST /api/embed` |
| `ollama.chat-model` | `qwen3:8b` | Model for `POST /api/chat` |
| `ollama.embedding-timeout-seconds` | `120` | Timeout for embedding calls |
| `ollama.chat-timeout-seconds` | `300` | Timeout for LLM answer generation |
| `ollama.timeout-seconds` | `120` | Fallback if specific timeouts not set |
