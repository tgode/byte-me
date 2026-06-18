# ByteHR AI — PDF Ingestion Audit

**Date:** 2026-06-18  
**Pipeline:** Local filesystem → Tika → Redaction → Chunking → nomic-embed-text → pgvector

---

## Pipeline Architecture

```
PDF / DOCX / TXT / MD
        │
        ▼ Apache Tika AutoDetectParser
   Text Extraction + Page Count
        │
        ▼ RedactionServiceImpl
   PII Redaction (email/phone/IBAN/NID)
        │
        ▼ LanguageDetectionService (Tika Optimaize)
   Language Detection (en / sq / sr)
        │
        ▼ ChunkingService
   1000-char chunks / 200-char overlap
        │
        ▼ EmbeddingService → OllamaClient → nomic-embed-text
   768-dimensional vector embedding per chunk
        │
        ▼ JdbcTemplate → PostgreSQL + pgvector
   document_chunks.embedding vector(768)
```

---

## Document Discovery

ByteHR scans all subdirectories of the configured local path recursively.

**Default path:** `sample-data/hr-documents/`

### Folder → Country mapping

| Folder pattern | Country code | Priority |
|---|---|---|
| `albania/`, `albania-docs/` | `AL` | Albanian queries search here first |
| `serbia/`, `sebia/`, `sebia-docs/` | `RS` | Serbian queries search here first |
| `global/`, anything else | `null` | Available to all employees |

### Supported file formats

| Extension | Format | Tika parser used |
|---|---|---|
| `.pdf` | PDF | PDFParser (Apache PDFBox) |
| `.docx` | Word document | OOXMLParser |
| `.txt` | Plain text | TXTParser |
| `.md` | Markdown | TXTParser (plain text) |

---

## Pre-Ingestion Redaction

Before any text enters the database or embedding model, `RedactionServiceImpl` applies these patterns:

| Pattern | Example input | Replacement |
|---|---|---|
| Email address | `hr@company.al` | `[EMAIL REDACTED]` |
| Phone number | `+355 4 123 456` | `[PHONE REDACTED]` |
| IBAN | `AL35202111090000000001234567` | `[IBAN REDACTED]` |
| Albanian NID | `A12345678B` | `[ID REDACTED]` |
| Albanian NIPT | `K12345678A` | `[ID REDACTED]` |
| Serbian JMBG | `1234567890123` | `[ID REDACTED]` |

Configuration: `bytehr.security.redaction.enabled=true` (default)

---

## Page Number Citation

Page numbers in citations are estimated from chunk position:

```
estimatedPage = (chunkIndex / totalChunks) × totalPages + 1
```

For PDFs, `totalPages` is read from Tika metadata (`xmpTPg:NPages`, `meta:page-count`, `Office.PAGE_COUNT`). Falls back to `1` if metadata is unavailable (e.g., plain text files).

Citation format: `Employee Handbook.pdf (Page 3)` — never exposes filesystem path.

---

## Deduplication / Change Detection

The ingestion pipeline uses the file's last-modified timestamp for change detection:

- **New file** (not in database): processed, embedded, stored
- **Modified file** (last-modified > stored): re-indexed, old chunks deleted
- **Unchanged file** (same last-modified): skipped entirely

To force full reindex: `POST /api/admin/reindex`

---

## Running the Audit

### Trigger ingestion

```bash
# Standard sync (change detection)
curl -X POST http://localhost:8080/api/sync

# Full reindex (clears all, reprocesses everything)
curl -X POST http://localhost:8080/api/admin/reindex
```

### Check indexing status

```sql
-- Total documents indexed
SELECT COUNT(*), country FROM documents GROUP BY country;

-- Chunk and embedding counts
SELECT
  d.name,
  d.language,
  d.country,
  COUNT(dc.id)             AS chunks,
  COUNT(dc.embedding)      AS embeddings,
  COUNT(dc.id) = COUNT(dc.embedding) AS fully_embedded
FROM documents d
LEFT JOIN document_chunks dc ON dc.document_id = d.id
GROUP BY d.id, d.name, d.language, d.country
ORDER BY d.name;

-- Any chunks missing embeddings?
SELECT COUNT(*) AS missing_embeddings
FROM document_chunks
WHERE embedding IS NULL;
```

### Expected log output during ingestion

```
[Processor] Processing document: 'vacation-policy.md' (1941 bytes)
[Processor] Extracted: 'vacation-policy.md' — chars=1847, pages=1
[Processor] PII redaction applied to 'vacation-policy.md'
[Processor] 'vacation-policy.md' → 3 chunks (size=1000, overlap=200)
[Embedding] Requesting chunk 1/3 of 'vacation-policy.md': chunkId=..., textLen=987, page=1
[Embedding] Received: dim=768
[Embedding] Persisted embedding for chunk id=...: rowsUpdated=1
[Processor] 'vacation-policy.md' complete: 3/3 embeddings OK, 0 failed
```

---

## Adding Real PDF Documents

1. Place PDF files in the appropriate country folder:
   - `sample-data/hr-documents/albania-docs/` → Albanian HR docs (`country=AL`)
   - `sample-data/hr-documents/sebia-docs/` → Serbian HR docs (`country=RS`)
   - `sample-data/hr-documents/global/` → Company-wide policies

2. Trigger reindex:
   ```bash
   curl -X POST http://localhost:8080/api/admin/reindex
   ```

3. Verify:
   ```bash
   curl -s -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"question":"What vacation entitlement do employees receive in Albania?","country":"AL"}'
   ```
