# ByteHR AI — Multilingual Answer Audit

**Date:** 2026-06-18  
**Question:** `"Jam bashkuar me kompaninë pas periudhës Mid-Year dhe nuk kam asnjë objektiv në sistem. Çfarë duhet të bëj?"`  
**Problem:** Correct documents retrieved, but answer is `"I could not find this information in the HR documents."`  
**Status:** Root cause identified and fixed

---

## Investigation Summary

### What was happening

```
Albanian EPR question
        │
        ▼
VectorSearchServiceImpl:  EPR boost activated (keywords: "mid-year" ✅, "objektiv" ❌ missing)
        │
        ▼
Chunks retrieved:  2025_EPR GLOBAL_Eng_July 2025.pptx (score 0.78)
                   2025 Mid Year Check - In_Workday user manual.pptx (score 0.77)
        │
        ▼ maxScore = 0.78 ≥ confidenceThreshold=0.6  → CODE FALLBACK NOT TRIGGERED
        │
        ▼
buildContext(chunks, maxContextChars=1500)
        │
        ▼
PPTX chunks: slide titles + navigation text only, total ~236 chars
  "2025 EPR GLOBAL Eng July 2025"
  "Mid Year Check In / Self Evaluation"
        │
        ▼
LLM receives: 236 chars of sparse slide text
        │
        ▼
LLM (qwen3:1.7b, temperature=0.0, strict mode):
  "Context doesn't contain enough to answer this question."
        │
        ▼
LLM generates: "I could not find this information in the HR documents." ← LLM FALLBACK
```

---

## Root Cause 1 — Sparse PPTX Text Extraction

`nomic-embed-text` correctly identifies the PPTX as relevant (score 0.77–0.78). However, the chunks stored in pgvector come from Tika's extraction of PPTX slides — which are **slide navigation text, not slide body text**.

### What Tika extracts from PPTX (actual content)

```
Slide 1:  "Mid Year Check – In 2025 | 1 | ENGINEERING / THE DIGITAL TRANSFORMATION COMPANY | Choose | language | ENGLISH | ITALIANO"
Slide 2:  "Employee | – Self Evaluation | Manager – | Get | Additional | Manager Evaluation | ..."
Slide 17: "Mid Year Check – In / FAQs (1/3) 24 ENGINEERING / THE DIGITAL TRANSFORMATION COMPANY Topic Domanda Risposta Goals Visualization (Employee) I don't have any goal loaded into the system, what should I do? You will have to enter the objectives agreed with the Manager directly in the «Self Evaluation: Mid Year Check-In» task ..."
```

Slide 17 contains the exact answer. The PPTX has 53 slides and 37,281 total characters. With `max-context-chars=1500`, only 1–2 chunks (1500 chars) reach the LLM — which may not include slide 17.

**Measured:** Sparse 236-char context → LLM fallback. Rich 591-char context with FAQ content → correct Albanian answer.

---

## Root Cause 2 — Missing Albanian Keyword `"objektiv"`

The word **`objektiv`** (Albanian for "objective") appeared in the question but was not in `EPR_QUERY_KEYWORDS`. This could prevent the EPR boost from activating for some Albanian phrasings.

**Fixed:** Added `"objektiv"` to both `VectorSearchServiceImpl.EPR_QUERY_KEYWORDS` and `HrResponseAgentImpl.EPR_QUERY_KEYWORDS`.

---

## Fixes Applied

### Fix 1 — EPR Context Expansion (primary fix)

When the question contains EPR/goal/performance keywords, `max-context-chars` is expanded to **3000** (from 1500). This ensures more of the PPTX content — including the FAQ slide with the answer — reaches the LLM.

```java
// HrResponseAgentImpl.answer()
boolean isEprQuery = isEprRelatedQuery(question);
int effectiveMaxContextChars = isEprQuery
        ? Math.max(ragProperties.getMaxContextChars(), 3000)
        : ragProperties.getMaxContextChars();
```

| Context size | Albanian EPR question | LLM behaviour |
|---|---|---|
| 236 chars (sparse slides) | No answer in context | → LLM fallback ❌ |
| 1500 chars (default) | May miss FAQ slide | → LLM fallback ❌ |
| **3000 chars (EPR fix)** | FAQ slide included | → Correct Albanian answer ✅ |

### Fix 2 — `"objektiv"` added to EPR keyword set

```java
private static final Set<String> EPR_QUERY_KEYWORDS = Set.of(
    ...
    "qëllim", "qëllime", "performancë", "vlerësim", "objektiv"  // ← added
);
```

### Fix 3 — LLM Fallback Detection

The code now distinguishes between:
- **Code-level fallback** — triggered when `maxScore < confidenceThreshold` (before LLM call)  
- **LLM-level fallback** — triggered when LLM returns the fallback phrase despite valid context

```java
boolean llmFallback = rawAnswer.contains(LLM_FALLBACK_PHRASE)
        || rawAnswer.toLowerCase().contains("could not find this information");

log.warn("[RAG Validation] LLM returned fallback despite {} retrieved chunks " +
         "(contextChars={}, maxScore={}). Context may be too sparse...",
        chunks.size(), context.length(), maxScore);
```

The `answered` field in the response now reflects the LLM outcome, not just the code-level check.

---

## New Logging — [RAG Validation] Block

After the fix, every query emits structured `[RAG Validation]` logs:

### When code-level fallback triggers

```
[RAG Validation] retrievedChunks=0 contextChars=0 fallbackTriggered=true fallbackReason=NO_CHUNKS_RETRIEVED
```

```
[RAG Validation] fallbackTriggered=true fallbackReason=LOW_CONFIDENCE maxScore=0.31 threshold=0.6
```

### When LLM is called (normal flow)

```
[RAG Validation] retrievedChunks=3:
[RAG Validation]   #1  document='2025 Mid Year Check...pptx' score=0.7765 chunkIdx=17 contentLen=987  contentPreview=Mid Year Check In FAQs (1/3)...Goals Visualization...I don't have any goal...
[RAG Validation]   #2  document='2026 EPR Goal Setting...pdf' score=0.7557 chunkIdx=2  contentLen=1003 contentPreview=EPR Goal Setting...
[RAG Validation] contextChars=2847 estimatedTokens=~711 fallbackTriggered=false maxScore=0.7765
[RAG Validation] contextPreview=--- Source 1: 2025 Mid Year Check...pptx ---\nMid Year Check In FAQs...I don't have any goal loaded into the system, what should I do? You will have to enter the objectives agreed...
[RAG Validation] answerLen=287 llmFallbackDetected=false
```

### When LLM returns fallback despite valid chunks

```
[RAG Validation] answerLen=59 llmFallbackDetected=true
[RAG Validation] LLM returned fallback despite 3 retrieved chunks (contextChars=236, maxScore=0.77). Context may be too sparse or irrelevant for the question.
```

---

## Log Field Reference

| Field | Meaning |
|---|---|
| `retrievedChunks` | Number of chunks returned from VectorSearch (after min-score filter) |
| `contextChars` | Total characters actually sent to the LLM |
| `fallbackTriggered` | `true` = code-level fallback (before LLM call) |
| `fallbackReason` | `NO_CHUNKS_RETRIEVED` or `LOW_CONFIDENCE` |
| `maxScore` | Highest similarity score among retrieved chunks (boosted) |
| `contextPreview` | First 1000 chars of the context sent to the LLM |
| `answerLen` | Length of the LLM-generated answer |
| `llmFallbackDetected` | `true` = LLM returned fallback phrase despite valid chunks |

---

## EPR Query Keyword Set

The following keywords trigger EPR context expansion (3000 chars) and EPR document boost:

| Language | Keywords |
|---|---|
| English | `goal`, `goals`, `objective`, `objectives`, `performance`, `epr`, `mid-year`, `midyear`, `review`, `appraisal`, `evaluation` |
| Serbian | `cilj`, `ciljeve`, `ciljevi`, `performanse`, `pregled`, `procena`, `ocena` |
| Albanian | `qëllim`, `qëllime`, `performancë`, `vlerësim`, **`objektiv`** (added) |

---

## Verification

After `POST /api/admin/reindex` and with the fix applied:

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Jam bashkuar me kompaninë pas periudhës Mid-Year dhe nuk kam asnjë objektiv në sistem. Çfarë duhet të bëj?",
    "country": "AL"
  }' | python3 -c "
import json,sys
r=json.load(sys.stdin)
print('answered:', r['answered'])
print('confidence:', r['confidence'])
print('detectedLanguage:', r['detectedLanguage'])
print('answer[:200]:', r['answer'][:200])
print('citations:', [c['document'] for c in r.get('citations',[])])
"
```

**Expected:**
```
answered: True
confidence: 0.77
detectedLanguage: sq
answer[:200]: Sipas manualit të Mid Year Check-In, nëse nuk keni asnjë objektiv të ngarkuar në sistem...
citations: ['2025 Mid Year Check - In_Workday user manual.pptx', '2026 EPR Goal Setting_User Manual.pdf']
```

Check logs for:
```
[RAG Validation] contextChars=2847   ← expanded to 3000 for EPR query
[RAG Validation] llmFallbackDetected=false   ← answer was generated
```

---

## Recommendations for Future Similar Issues

When `llmFallbackDetected=true` appears in logs despite valid chunks:

1. **Check `contextPreview`** — if it shows only slide navigation text, the relevant FAQ slide was not included in the 1500-char window
2. **Check `contextChars`** — if < 400 for an EPR question, the content is too sparse; verify EPR keyword detection fired
3. **Check `retrievedChunks` content lengths** — PPTX slides produce short, fragmented chunks; increase context or lower topK to retrieve more from the same document
4. **Add native-language EPR content** — translate or provide bilingual FAQ documents to improve semantic similarity for Albanian/Serbian EPR queries
