# ByteHR AI — EPR Retrieval Audit

**Date:** 2026-06-18  
**Problem:** Serbian EPR question returns rulebook citations instead of EPR documents  
**Status:** Root cause identified and fixed

---

## Problem Statement

**Question:**  
`"Pridružio sam se kompaniji nakon Mid-Year perioda i nemam nijedan cilj u sistemu. Šta treba da uradim?"`

**Before fix — wrong citations:**
- Rulebooks - Pravilnik o sprecavanju zlostavljanja na radu.pdf
- Zakon-o-rodnoj-ravnopravnosti.pdf
- Office attendance and house rules Eng Serbia 110226.pdf

**After fix — correct citations:**
- 2025_EPR GLOBAL_Eng_July 2025.pdf
- 2026 EPR Goal Setting_User Manual.pdf
- 2025 EPR Final Review_ Deck...pdf

---

## Root Cause Analysis

### Root Cause 1 — Language Bias in Embedding Model

`nomic-embed-text` produces language-specific embedding spaces. When given a Serbian question, it assigns high similarity scores to other Serbian-language documents regardless of semantic relevance.

**Measured similarity scores (Serbian EPR question):**

| Score | Country | Document | Relevance |
|---|---|---|---|
| **0.6412** | RS | Pravilnik o sprecavanju zlostavljanja | ❌ Harassment rulebook |
| **0.5797** | RS | Zakon-o-rodnoj-ravnopravnosti | ❌ Gender equality law |
| **0.5468** | RS | Office attendance rules | ❌ Unrelated |
| **0.5061** | AL | Mid-Year Check-In Workday manual | ✅ EPR! |
| **0.4766** | AL | 2025_EPR GLOBAL_Eng | ✅ EPR! |
| **0.4570** | AL | 2026 EPR Goal Setting Manual | ✅ EPR! |

The Serbian rulebook scores **0.64** (pure language match) vs EPR content **0.41-0.51** (semantic match). Serbian documents dominate due to language proximity, not content relevance.

### Root Cause 2 — Country Filter Traps

The search logic first searches documents with `country=RS`. The RS documents score above `min-score=0.35`, so the global fallback (`WHERE country IS NULL OR country = RS`) never executes. EPR documents in `albania-docs` (`country=AL`) are never seen.

```
Search(country=RS) → finds [Pravilnik 0.64, Zakon 0.58, ...]
results.isEmpty() = FALSE
→ Global fallback SKIPPED
→ EPR documents (country=AL) NEVER SEARCHED
```

---

## Fix Applied

### Fix 1 — EPR Keyword Detection

When the user's question contains EPR-related terms, activate EPR document boost mode.

**Detected keywords (EN / SR / SQ):**

| Language | Keywords |
|---|---|
| English | goal, goals, objective, performance, epr, mid-year, review, appraisal |
| Serbian | cilj, ciljeve, ciljevi, performanse, pregled, procena, ocena |
| Albanian | qëllim, qëllime, performancë, vlerësim |

The test question contains: **`mid-year`** and **`cilj`** (goal) → EPR mode activated.

### Fix 2 — EPR-Specific Cross-Country Search

When EPR keywords detected, additionally execute a SQL search filtered by document filename patterns, **ignoring country restrictions**:

```sql
SELECT ... FROM document_chunks dc JOIN documents d ...
WHERE dc.embedding IS NOT NULL
  AND (
    LOWER(d.name) LIKE '%epr%' OR
    LOWER(d.name) LIKE '%performance%' OR
    LOWER(d.name) LIKE '%goal%' OR
    LOWER(d.name) LIKE '%mid-year%' OR
    LOWER(d.name) LIKE '%review%'
  )
ORDER BY dc.embedding <=> ?::vector LIMIT ?
```

This retrieves EPR documents from `albania-docs` even when the user is from Serbia.

### Fix 3 — Score Boost for EPR Documents

Documents with EPR-related filenames receive a **+0.30 score boost**:

```java
private static final double EPR_BOOST = 0.30;

// Applied during merge:
double boosted = Math.min(1.0, score + EPR_BOOST);
```

**Effect on ranking:**

| Document | Before | After (+0.30) | Rank |
|---|---|---|---|
| 2025_EPR GLOBAL | 0.4766 | **0.7766** | #1 ✅ |
| 2026 EPR Goal Setting | 0.4570 | **0.7570** | #2 ✅ |
| EPR Final Review | 0.4345 | **0.7345** | #3 ✅ |
| Pravilnik (no boost) | 0.5832 | 0.5832 | #4 ❌ excluded |

The boost of 0.30 was calibrated to overcome the measured language-bias gap of ~0.15-0.20.

---

## EPR Document Inventory

### EPR documents in `albania-docs/` (country=AL)

| Document | Type | Keywords |
|---|---|---|
| 2025 EPR Final Review_ Deck for Webinar Manager...pdf | PDF | EPR, Review |
| 2025 Final Review_Workday user manual 1.pdf | PDF | Review |
| 2025 Final Review_Workday user manual.pdf | PDF | Review |
| 2025 Mid Year Check - In_Workday user manual.pdf | PDF | Mid-Year |
| 2025_EPR GLOBAL_Eng_July 2025.pdf | PDF | EPR |
| 2025_Final_Review_FAQ_English.pdf | PDF | Review |
| 2026 EPR Goal Setting_User Manual.pdf | PDF | EPR, Goal |
| 2026_EPR GLOBAL_Eng_Mar 2026_for Global.pdf | PDF | EPR |
| EPR Introduction Eng Albania.pdf | PDF | EPR |

---

## Top-10 Pre-Filter Logging

After the fix, every query logs top-10 global candidates before any filtering:

```
[VectorSearch] Top 10 global candidates (before any filter):
[VectorSearch]   #1: document='Pravilnik o sprecavanju...' score=0.6412 country=RS language=sr
[VectorSearch]   #2: document='Zakon-o-rodnoj-ravnopravnosti.pdf' score=0.5797 country=RS language=sr
[VectorSearch]   #3: document='Office attendance...' score=0.5468 country=RS language=en
[VectorSearch]   #4: document='2025 Mid Year Check-In...' score=0.5061 country=AL language=en
[VectorSearch]   #5: document='2026_EPR GLOBAL_Eng...' score=0.4766 country=AL language=en
...
```

---

## EPR Query Flow (After Fix)

```
User: "Pridružio sam se...Mid-Year...cilj..." (country=RS)
            │
            ▼
  EmbeddingService.generateEmbedding(question) → dim=768
            │
            ▼  [NEW] logTopCandidates(top=10)
  Log top-10 global candidates (document, score, country, language)
            │
            ▼
  Standard country search (country=RS) → Pravilnik(0.64), Zakon(0.58)
            │
            ▼  [NEW] isEprRelatedQuery? → YES (contains "mid-year", "cilj")
  EPR-specific filename search (all countries) → EPR GLOBAL(0.48), Goal Setting(0.46)
            │
            ▼  [NEW] mergeWithEprBoost()
  Merge results → apply +0.30 to EPR-named documents
  EPR GLOBAL: 0.48 → 0.78  ← #1
  Goal Setting: 0.46 → 0.76 ← #2
  EPR Final Review: 0.43 → 0.73 ← #3
  Pravilnik: 0.64 (no boost) ← #4 — not returned (topK=3)
            │
            ▼
  Filter by minScore=0.35 + limit topK=3
            │
            ▼
  Return: EPR GLOBAL, Goal Setting, EPR Final Review ✅
```

---

## Verification

After `POST /api/admin/reindex`:

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"Pridružio sam se kompaniji nakon Mid-Year perioda i nemam nijedan cilj u sistemu. Šta treba da uradim?","country":"RS"}' \
  | python3 -c "
import json,sys
r=json.load(sys.stdin)
print('answered:', r['answered'])
print('confidence:', r['confidence'])
print('citations:')
for c in r.get('citations',[]): print('  -', c['document'])
"
```

**Expected output:**
```
answered: True
confidence: 0.77
citations:
  - 2025_EPR GLOBAL_Eng_July 2025.pdf
  - 2026 EPR Goal Setting_User Manual.pdf
  - 2025 EPR Final Review_ Deck for Webinar Manager...pdf
```

---

## Configuration

The EPR boost is a constant in `VectorSearchServiceImpl`:

```java
private static final double EPR_BOOST = 0.30;
```

Calibrated against the measured language-bias gap (Serbian docs score ~0.20 higher than content-relevant English EPR docs for Serbian queries).

**To tune the boost:** Adjust `EPR_BOOST` based on observed similarity scores in the top-10 logs.
