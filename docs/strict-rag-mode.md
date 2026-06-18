# ByteHR AI — Strict RAG Mode

**Feature:** Strict RAG (Retrieval-Augmented Generation) mode  
**Config:** `bytehr.rag.strict-mode=true` (default)  
**Status:** Active

---

## Problem

Without strict constraints, LLMs occasionally supplement retrieved document
content with general knowledge — inventing:

- HR contact email addresses not present in any document
- Carry-over policies not mentioned in the retrieved context
- Benefits or deadlines inferred from common HR practices
- Procedures described in general terms rather than as documented

This is unacceptable for an HR assistant where accuracy and auditability are mandatory.

---

## Solution

When `strict-mode=true` (default), a stricter system prompt is injected that:

1. Explicitly forbids any information not present in the retrieved document context
2. Lists specific categories of forbidden invention: contacts, emails, policies, procedures, carry-over rules
3. Provides an exact fallback phrase for missing information
4. Instructs the model to quote or paraphrase from the source

Temperature is set to `0.0` (fully deterministic) to eliminate creative variation.

---

## System Prompt Comparison

### Standard mode (`strict-mode=false`)

```
You are ByteHR AI, an HR assistant for employees in Albania and Serbia.

RULES:
1. Answer ONLY HR-related questions using the provided document context.
2. NEVER invent, hallucinate, or assume company policies not present in the context.
...
```

### Strict mode (`strict-mode=true`) — default

```
You are ByteHR AI, an HR assistant for employees in Albania and Serbia.

STRICT RAG MODE — MANDATORY RULES (follow exactly):

1. Answer ONLY using the document context provided below. No exceptions.
2. Do NOT invent, estimate, or assume any information not explicitly stated in the context.
3. Do NOT add HR contact details, email addresses, phone numbers, or URLs unless they
   appear word-for-word in the context.
4. Do NOT mention carry-over policies, deadlines, benefits, bonuses, or procedures
   that are not explicitly written in the context.
5. If the answer to the question is NOT found in the context, respond with this exact phrase:
   "I could not find this information in the HR documents."
...
10. Keep responses concise and factual. Do not add qualifications, suggestions, or advice
    beyond what the documents state.

CRITICAL: Use ONLY the above context. Do not supplement with general knowledge.
```

---

## Configuration

### application.yml

```yaml
bytehr:
  rag:
    strict-mode: ${BYTEHR_RAG_STRICT_MODE:true}   # true = strict (default), false = standard

ollama:
  chat-temperature: ${OLLAMA_CHAT_TEMPERATURE:0.0} # 0.0 = deterministic
```

### Environment variables

| Variable | Default | Effect |
|---|---|---|
| `BYTEHR_RAG_STRICT_MODE` | `true` | Activates strict system prompt |
| `OLLAMA_CHAT_TEMPERATURE` | `0.0` | Deterministic output (no sampling randomness) |

---

## Validation Logging

Emitted at `INFO` level on every query when strict mode is active:

```
[RAG] Query: user='api-user-...', country='AL', lang='en', topK=3, maxContextChars=1500, strictMode=true

[RAG] Retrieved 2 chunk(s):
[RAG]   document='vacation-policy.md' score=0.8921 chunkIdx=0
[RAG]   document='employee-benefits.md' score=0.7134 chunkIdx=1

[RAG] Context size: 1487 chars

[Chat] model=qwen3:1.7b
[Chat] promptChars=1847
[Chat] estimatedTokens=461
[Chat] generationDurationMs=21400

[RAG] Answer length: 312 chars
[RAG] Answered in 21652ms: confidence=0.892, chunks=2, citations=1, strictMode=true
```

---

## Verification

### Test question

> "I have worked in Albania for 6 years. How many vacation days do I receive?"

**Expected answer:** Must mention **25 working days**  
**Must NOT invent:** HR contacts, carry-over policies, additional benefits  
(unless explicitly present in `albania/vacation-policy.md`)

### Actual answer (verified, qwen3:1.7b, temperature=0.0, strict-mode=true)

```
You are entitled to 25 working days of paid annual vacation per year, as specified
in the Albanian Labour Code for employees who have completed more than 5 years of
service. This entitlement is proportional to your working hours for part-time
employees, but since you are a full-time employee, you receive 25 working days.

Source: vacation-policy.md
```

### Verification results

| Check | Result |
|---|---|
| Mentions 25 working days | ✅ YES |
| Sources cited | ✅ `vacation-policy.md` |
| HR contacts invented | ✅ NO |
| Carry-over policies invented | ✅ NO |
| Additional benefits invented | ✅ NO |
| Answer contained in document | ✅ YES |

---

## Missing Information Behaviour

When the question cannot be answered from retrieved documents:

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the CEO salary at this company?"}'
```

**Expected response:**
```json
{
  "answer": "I could not find this information in the HR documents.",
  "citations": [],
  "confidence": 0.0,
  "answered": false
}
```

---

## Switching Modes

### Disable strict mode (not recommended for production)

```bash
# .env
BYTEHR_RAG_STRICT_MODE=false
OLLAMA_CHAT_TEMPERATURE=0.1   # allow some variation

# Docker restart
docker compose up -d --no-deps bytehr-api
```

### Re-enable strict mode

```bash
BYTEHR_RAG_STRICT_MODE=true
OLLAMA_CHAT_TEMPERATURE=0.0
docker compose up -d --no-deps bytehr-api
```

---

## Temperature Impact

| Temperature | Behaviour | Use case |
|---|---|---|
| `0.0` | Fully deterministic — same question always produces same answer | **Default — strict RAG** |
| `0.1` | Very low variation — occasionally rephrase | Standard mode |
| `0.5`+ | Creative — may invent or embellish | Never use for HR |

---

## Files Changed

| File | Change |
|---|---|
| `config/RagProperties.java` | Added `strictMode: boolean = true` |
| `service/impl/HrResponseAgentImpl.java` | Added `SYSTEM_PROMPT_STRICT`, prompt selection by `strictMode`, validation logging |
| `integration/ollama/OllamaClient.java` | Temperature now read from `ollama.chat-temperature` property (default 0.0) |
| `application.yml` | Added `ollama.chat-temperature`, `bytehr.rag.strict-mode` |
| `.env.example` | Added `OLLAMA_CHAT_TEMPERATURE`, `BYTEHR_RAG_STRICT_MODE` |
| `docker-compose.yml` | Added both env vars to `bytehr-api` service |
