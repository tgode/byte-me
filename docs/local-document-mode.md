# ByteHR AI — Local Document Mode

**Mode:** Hackathon / MVP  
**Status:** Default active mode

---

## Overview

ByteHR AI supports two document source modes:

| Mode | Value | Use Case |
|---|---|---|
| **Local** (default) | `local` | Hackathon MVP, demos, local development |
| **SharePoint** | `sharepoint` | Production deployment |

In local mode, HR documents are loaded from the local filesystem instead of SharePoint.  
The SharePoint integration code remains intact and can be activated at any time by changing one configuration value.

---

## Architecture

```
sample-data/hr-documents/
        │
        ▼
LocalDocumentSourceServiceImpl
        │  (same pipeline as SharePoint mode)
        ▼
DocumentProcessorService
  ├── Apache Tika (text extraction)
  ├── LanguageDetectionService (Tika Optimaize)
  ├── ChunkingService (1000 chars / 200 overlap)
  ├── EmbeddingService (Ollama nomic-embed-text)
  └── pgvector storage (JdbcTemplate)
        │
        ▼
VectorSearchService
        │
        ▼
HrResponseAgent (Ollama qwen3:8b)
        │
        ▼
Teams Bot Response
```

The ingestion pipeline is **identical** in both modes. Only the document acquisition layer differs.

---

## Directory Structure

```
sample-data/
└── hr-documents/
    ├── albania/
    │   ├── vacation-policy.md          (20 days, carry-over rules, Albanian specifics)
    │   ├── employee-benefits.md        (health insurance, meal allowance, gym, pension)
    │   └── sick-leave-policy.md        (ISSH rules, medical certificate requirements)
    ├── serbia/
    │   ├── vacation-policy.md          (20+ days by seniority, regres, Serbian labour law)
    │   ├── employee-benefits.md        (RFZO, topli obrok, pension Stub III)
    │   └── sick-leave-policy.md        (RFZO, doznaka, compensation rates)
    └── global/
        ├── remote-work-policy.md       (2 days/week, core hours, internet allowance)
        ├── public-holidays-2026.md     (full holiday lists for Albania and Serbia)
        ├── onboarding-guide.md         (first week schedule, probation, key contacts)
        ├── travel-reimbursement.md     (per diem, hotel limits, non-reimbursables)
        ├── equipment-request.md        (laptop refresh, home office, approval thresholds)
        └── payroll-information.md      (payroll dates, tax rates, salary review)
```

**11 documents**, covering all major HR topics from the business specification.

---

## Configuration

### application.yml

```yaml
bytehr:
  source:
    type: ${BYTEHR_SOURCE_TYPE:local}          # "local" or "sharepoint"
    local-path: ${BYTEHR_LOCAL_PATH:./sample-data/hr-documents}
    sync-interval-ms: ${BYTEHR_SYNC_INTERVAL_MS:3600000}
```

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `BYTEHR_SOURCE_TYPE` | `local` | Source mode: `local` or `sharepoint` |
| `BYTEHR_LOCAL_PATH` | `./sample-data/hr-documents` | Filesystem path to scan |
| `BYTEHR_SYNC_INTERVAL_MS` | `3600000` | Auto-sync interval in milliseconds (1 hour) |

---

## Bean Activation

Spring Boot's `@ConditionalOnProperty` ensures only one `DocumentSyncService` bean is active:

```
BYTEHR_SOURCE_TYPE=local       →  LocalDocumentSourceServiceImpl  (active)
                                   DocumentSyncServiceImpl         (inactive)

BYTEHR_SOURCE_TYPE=sharepoint  →  LocalDocumentSourceServiceImpl  (inactive)
                                   DocumentSyncServiceImpl         (active)
```

The `SyncController`, `HrResponseAgent`, and all other components are completely unaffected.

---

## Country Detection

Documents are automatically tagged with a country code based on folder name:

| Folder | Country Code | Policy priority |
|---|---|---|
| `albania/` | `AL` | Albanian employees see these first |
| `serbia/` | `RS` | Serbian employees see these first |
| `global/` | `null` | Available to all employees |

The `VectorSearchService` first searches country-specific documents, then falls back to global documents if no results are found.

---

## Duplicate Protection

The sync service tracks documents using a stable unique ID:

```
local://albania/vacation-policy.md
local://global/remote-work-policy.md
```

On each sync run:
- If the file ID is **not in the database** → process as new
- If the file ID **exists** and the file's last-modified time is **unchanged** → skip (no reprocessing)
- If the file ID **exists** and the file has been **modified** → re-index with fresh embeddings

---

## Supported File Formats

| Format | Extension | Handler |
|---|---|---|
| Markdown | `.md` | Apache Tika (plain text) |
| Plain text | `.txt` | Apache Tika (plain text) |
| PDF | `.pdf` | Apache Tika (PDF parser) |
| Word Document | `.docx` | Apache Tika (OOXML parser) |

---

## Startup Validation

On every application start, ByteHR logs a validation banner:

```
╔══════════════════════════════════════════════╗
║              ByteHR AI  —  Startup           ║
╠══════════════════════════════════════════════╣
║  Source Type  : LOCAL                        ║
║  Source Path  : sample-data/hr-documents     ║
║  Path Exists  : YES                          ║
║  Files Found  : 11                           ║
║  Indexed Docs : 0                            ║
╚══════════════════════════════════════════════╝
```

---

## Local Development Setup

### 1. Prerequisites

- Docker Desktop running
- At least 8 GB RAM (for Ollama models)

### 2. Start the stack

```bash
cp .env.example .env
# BYTEHR_SOURCE_TYPE=local is already the default — no changes needed

docker compose up -d
```

### 3. Wait for models

```bash
# Watch the model download progress
docker compose logs -f ollama-init
# Expected: "nomic-embed-text" and "qwen3:8b" pulled successfully
```

### 4. Trigger document indexing

```bash
curl -X POST http://localhost:8080/api/sync
```

Expected response:
```json
{
  "status": "success",
  "documentsProcessed": 11,
  "totalDocumentsIndexed": 11,
  "durationMs": 45230,
  "sourceType": "local",
  "message": "11 document(s) processed. 11 total indexed."
}
```

### 5. Verify indexing

```bash
# Check health
curl http://localhost:8080/actuator/health

# Re-run sync to verify deduplication (should process 0 documents)
curl -X POST http://localhost:8080/api/sync
# → "documentsProcessed": 0
```

---

## Demo Workflow

### Basic HR question

Send a message to the ByteHR AI bot in Teams or test directly:

```bash
curl -s -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "type": "message",
    "text": "How many vacation days do I have?",
    "from": {"id": "demo-user", "name": "Demo Employee"},
    "conversation": {"id": "demo-conv-1"}
  }' | python3 -m json.tool
```

### Country-specific query (Albanian)

```bash
curl -s -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "type": "message",
    "text": "Sa dite pushimi kam ne vit?",
    "from": {"id": "user-al", "name": "Arben Hoxha"},
    "conversation": {"id": "demo-conv-2"}
  }'
```

### Country-specific query (Serbian)

```bash
curl -s -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "type": "message",
    "text": "Koliko dana godisnjeg odmora imam?",
    "from": {"id": "user-rs", "name": "Milica Petrovic"},
    "conversation": {"id": "demo-conv-3"}
  }'
```

### Demo scenario (full)

1. **Vacation question:** "How many vacation days do I have?"
2. **Follow-up:** "Can I carry them over to next year?"  ← tests conversation context
3. **Sick leave:** "What happens if I'm sick during vacation?"
4. **Remote work:** "Can I work from home?"
5. **Benefits:** "What health insurance does the company provide?"
6. **Off-topic (should be rejected):** "What is the weather in Tirana?"
7. **Albanian:** "Sa ditë pushimi kam?"
8. **Serbian:** "Koliko dana godišnjeg odmora imam?"

---

## Adding New Documents

1. Add the file to `sample-data/hr-documents/` (or a subdirectory)
2. Run sync: `curl -X POST http://localhost:8080/api/sync`
3. The document is automatically extracted, chunked, embedded, and indexed

No restart required. Supports `.md`, `.txt`, `.pdf`, `.docx`.

---

## Switching to SharePoint (Production)

```bash
# In .env
BYTEHR_SOURCE_TYPE=sharepoint
SHAREPOINT_TENANT_ID=...
SHAREPOINT_CLIENT_ID=...
SHAREPOINT_CLIENT_SECRET=...
SHAREPOINT_SITE_ID=...
SHAREPOINT_DRIVE_ID=...
SHAREPOINT_SYNC_ENABLED=true

# Restart the API container
docker compose up -d --no-deps bytehr-api
```

See [docs/deployment/sharepoint-setup.md](deployment/sharepoint-setup.md) for full setup.
