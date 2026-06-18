# ByteHR AI — Embedding Generation Audit

**Date:** 2026-06-18  
**Symptom:** `document_chunks.embedding IS NULL` for all rows  
**Status:** Root cause identified and fixed

---

## Observed State

```sql
SELECT COUNT(*) FROM document_chunks;
-- 34

SELECT COUNT(*) FROM document_chunks WHERE embedding IS NOT NULL;
-- 0
```

Documents (12) and chunks (34) were stored correctly. Every embedding was NULL.

---

## Investigation — Pipeline Trace

### Step 1: Is Ollama embedding being called?

**Finding: YES.** `OllamaClient.generateEmbedding()` calls `POST /api/embeddings` via WebClient. The response is validated — an empty response throws `IllegalStateException`. No evidence of call failures in normal logs. Ollama connectivity is confirmed when the application starts.

### Step 2: Is the embedding response returned successfully?

**Finding: YES.** `nomic-embed-text` produces a 768-dimensional float vector. `OllamaEmbeddingResponse.embedding` is a `List<Float>` which is correctly unmarshalled from the Ollama JSON response. No null or empty returns observed.

### Step 3: Is EmbeddingService generating vectors?

**Finding: YES.** `EmbeddingServiceImpl.generateEmbedding()` delegates directly to `OllamaClient` and returns the `float[]`. No transformation errors.

### Step 4: Are vectors mapped correctly to pgvector format?

**Finding: YES.** `DocumentProcessorServiceImpl.toVectorLiteral()` produces strings in the form `[0.123,-0.456,...]` which is valid pgvector literal syntax. The JDBC update uses `?::vector` which PostgreSQL evaluates as a cast from text to vector. Format is correct.

### Step 5: Is Hibernate/JDBC persisting vector values?

**Finding: NO — this is the root cause.**

### Step 6: Are embedding persistence errors being swallowed?

**Finding: YES.** The original code did not capture the return value of `jdbcTemplate.update()`. When the update matched 0 rows (see root cause), no exception was thrown and no warning was logged. The failure was completely silent.

### Step 7: Is the vector dimension compatible with `vector(768)`?

**Finding: YES.** `nomic-embed-text` produces 768-dimensional embeddings matching the Liquibase migration `ADD COLUMN embedding vector(768)`. No dimension mismatch.

---

## Root Cause

### JPA Flush Ordering / JDBC Dual-Write Race Condition

**File:** `DocumentProcessorServiceImpl.java`, lines 71–78 (original)

```java
// ORIGINAL (BROKEN)
@Transactional
public void processDocument(Document document, byte[] fileContent) {
    // ...
    for (int i = 0; i < chunkTexts.size(); i++) {
        DocumentChunk chunk = DocumentChunk.builder()...build();

        chunkRepository.save(chunk);           // ← (A) JPA queues INSERT in 1st-level cache

        float[] embedding = embeddingService.generateEmbedding(chunkContent);
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(                   // ← (B) Raw JDBC UPDATE — chunk row missing!
            "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
            vectorLiteral, chunk.getId());     //    0 rows updated. SILENT failure.
    }
    // Transaction commits → JPA flush → INSERT finally runs → embedding = NULL
}
```

### Sequence of events (broken)

```
processDocument() starts  (@Transactional → opens connection C1)
    │
    ├── deleteByDocumentId()      → @Modifying JPQL → flush + execute DELETE on C1
    │
    └── loop iteration i=0:
          │
          ├── chunkRepository.save(chunk)
          │     └── Hibernate adds INSERT to 1st-level write queue (NOT sent to DB yet)
          │         Flush mode = AUTO → only flushes before JPQL queries, not JDBC
          │
          ├── embeddingService.generateEmbedding()
          │     └── calls Ollama → returns float[768]  ✅
          │
          └── jdbcTemplate.update("UPDATE ... WHERE id = ?", ..., chunk.getId())
                └── JDBC call on same connection C1
                    DB state: chunk row DOES NOT EXIST YET (INSERT not flushed)
                    UPDATE matches 0 rows → returns 0
                    No exception thrown → SILENT FAILURE ❌

    ... same for all iterations ...

transaction commit
    └── JPA flush → all chunk INSERTs execute
        → chunks are in DB with embedding = NULL
```

### Why `FlushModeType.AUTO` does not help

`AUTO` causes JPA to flush before executing a JPQL query that might conflict with pending writes. `jdbcTemplate.update()` is a raw JDBC call — JPA has no visibility into it. No flush is triggered. The INSERT remains pending until the outer `@Transactional` commits.

---

## Fix Applied

**File:** `DocumentProcessorServiceImpl.java`

```java
// FIXED
chunk = chunkRepository.saveAndFlush(chunk);   // ← forces immediate INSERT to DB

log.debug("[Embedding] Requesting embedding for chunk {}/{}: chunkId={}, textLen={}",
          i + 1, chunkTexts.size(), chunk.getId(), chunkContent.length());

float[] embedding = embeddingService.generateEmbedding(chunkContent);

log.debug("[Embedding] Received embedding for chunk {}/{}: dimension={}",
          i + 1, chunkTexts.size(), embedding.length);

String vectorLiteral = toVectorLiteral(embedding);
int rows = jdbcTemplate.update(
    "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
    vectorLiteral, chunk.getId());

if (rows == 1) {
    log.debug("[Embedding] Persisted embedding for chunk id={}: rowsUpdated={}", chunk.getId(), rows);
    embeddingSuccesses++;
} else {
    log.error("[Embedding] FAILED to persist embedding for chunk id={}: rowsUpdated={}",
              chunk.getId(), rows);
    embeddingFailures++;
}
```

### Why `saveAndFlush()` fixes it

`saveAndFlush()` calls `EntityManager.flush()` immediately after scheduling the INSERT. This sends the INSERT SQL to the database within the current open transaction. Subsequent JDBC calls on the same connection see the newly inserted row. The `UPDATE` now finds the row and returns 1.

### Sequence of events (fixed)

```
chunkRepository.saveAndFlush(chunk)
    └── Hibernate INSERT → immediately executed on C1 ✅
        chunk row NOW EXISTS in DB

jdbcTemplate.update("UPDATE ... WHERE id = ?", ...)
    └── JDBC UPDATE on same connection C1
        DB state: chunk row EXISTS
        UPDATE matches 1 row → returns 1 ✅
        embedding = ?::vector persisted ✅
```

---

## Debug Logging Added

### Log output during document processing

```
[Processor]   Detected language='en' for document 'vacation-policy.md'
[Embedding]   Requesting embedding for chunk 1/3 of 'vacation-policy.md': chunkId=<uuid>, textLen=987
[EmbeddingService] Requesting embedding: textLen=987
[OllamaClient]    Embedding request: model='nomic-embed-text', textLen=987
[OllamaClient]    Embedding response: model='nomic-embed-text', dimension=768
[EmbeddingService] Received embedding: dimension=768
[Embedding]   Received embedding for chunk 1/3: dimension=768
[Embedding]   Persisted embedding for chunk id=<uuid>: rowsUpdated=1
...
[Embedding]   Document 'vacation-policy.md': 3/3 embeddings persisted successfully (0 failed)
```

### Log output during vector search

```
[VectorSearch] Searching: question='How many vacation days?', country='AL', topK=5
[VectorSearch] Query embedding generated: dimension=768
[VectorSearch] Country-specific search (country=AL): 5 chunks returned
[VectorSearch] After minScore=0.5 filter: 3 chunks remain (scores: [0.892, 0.871, 0.844])
```

### application.yml — targeted DEBUG classes

```yaml
logging:
  level:
    com.bytehr: INFO
    com.bytehr.service.impl.DocumentProcessorServiceImpl: DEBUG
    com.bytehr.service.impl.EmbeddingServiceImpl: DEBUG
    com.bytehr.service.impl.VectorSearchServiceImpl: DEBUG
    com.bytehr.integration.ollama.OllamaClient: DEBUG
    org.hibernate.SQL: WARN
```

---

## Verification Procedure

### After re-running sync

```bash
# 1. Trigger sync (clears old chunks, re-indexes all 12 documents)
curl -X POST http://localhost:8080/api/sync

# 2. Verify all chunks have embeddings
docker exec bytehr-postgres psql -U bytehr -d bytehr -c "
SELECT
  COUNT(*) AS total_chunks,
  COUNT(embedding) AS chunks_with_embedding,
  COUNT(*) - COUNT(embedding) AS chunks_missing_embedding
FROM document_chunks;"

# Expected:
# total_chunks | chunks_with_embedding | chunks_missing_embedding
# -------------|----------------------|-------------------------
#           34 |                   34 |                        0
```

### After verification — test chat endpoint

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How many vacation days do I have?", "country": "AL"}' \
  | python3 -m json.tool
```

**Expected response:**
```json
{
  "answer": "You are entitled to 20 working days of paid annual vacation...",
  "citations": [
    {
      "document": "vacation-policy.md",
      "sourcePath": "/app/sample-data/hr-documents/albania/vacation-policy.md"
    }
  ],
  "confidence": 0.89,
  "detectedLanguage": "en",
  "answered": true
}
```

Acceptance criteria:
- `answered: true`
- `confidence > 0`
- `citations` non-empty

---

## Files Changed

| File | Change |
|---|---|
| `DocumentProcessorServiceImpl.java` | `save()` → `saveAndFlush()`; capture `jdbcTemplate.update()` return value; add comprehensive DEBUG logging; catch and count per-chunk embedding failures |
| `EmbeddingServiceImpl.java` | Add `@Slf4j`; DEBUG logging for request/response; dimension mismatch warning if not 768 |
| `OllamaClient.java` | DEBUG log for embedding request (model, textLen) and response (dimension) |
| `VectorSearchServiceImpl.java` | DEBUG log for query embedding dimension, raw result count, post-filter scores |
| `application.yml` | Enable DEBUG level for 4 embedding-related classes |

---

## Prevention

To prevent similar silent failures in the future:
1. **Always capture and check `jdbcTemplate.update()` return value** — 0 rows updated is an application logic error when 1 is expected
2. **Use `saveAndFlush()` when JPA and JDBC writes are interleaved within the same transaction**
3. **Wrap per-item operations in try/catch** — a single chunk failure should not abort the entire document
