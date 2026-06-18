# ByteHR AI — Endpoint Analysis

**Date:** 2026-06-18  
**Analyst:** GitHub Copilot  
**Scope:** All REST controllers, service interfaces, DTOs, and security configuration

---

## Complete Endpoint Inventory

The application exposes **three HTTP endpoints** plus Spring Actuator.

| # | URL | Method | Controller | Auth | Purpose |
|---|---|---|---|---|---|
| 1 | `/api/messages` | POST | `TeamsController` | Public (Teams JWT) | Teams Bot webhook — full RAG pipeline entry point |
| 2 | `/api/sync` | POST | `SyncController` | Public | Trigger document synchronisation |
| 3 | `/actuator/health` | GET | Spring Actuator | Public | Container health probe |
| 4 | `/actuator/info` | GET | Spring Actuator | Public | Application info |

> **Security note:** All three application endpoints are declared `permitAll()` in `SecurityConfig`.
> In production, `/api/messages` should validate the Bot Framework JWT signed by Microsoft.

---

## Endpoint 1 — Teams Webhook (RAG Entry Point)

### Identification

| Attribute | Value |
|---|---|
| **Type** | Teams Bot webhook (Bot Framework Activity protocol) |
| **URL** | `POST /api/messages` |
| **Controller** | `com.bytehr.api.TeamsController` |
| **Authentication** | `permitAll()` — CSRF disabled |
| **Content-Type** | `application/json` |

### Request DTO — `TeamsActivity`

`com.bytehr.api.dto.TeamsActivity`

```json
{
  "type": "message",
  "id": "string",
  "text": "How many vacation days do I have?",
  "from": {
    "id": "string",
    "name": "Employee Name",
    "aadObjectId": "azure-ad-object-id"
  },
  "conversation": {
    "id": "conversation-id",
    "isGroup": false
  },
  "channelId": "msteams",
  "serviceUrl": "https://smba.trafficmanager.net/...",
  "replyToId": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | String | Yes | Activity type. Only `"message"` triggers RAG. Other types receive empty ACK. |
| `text` | String | Yes | The user's question / message text |
| `from.id` | String | No | Bot Framework user ID |
| `from.name` | String | No | Display name used in conversation history |
| `from.aadObjectId` | String | No | Azure AD Object ID — preferred user identifier |
| `conversation.id` | String | No | Conversation thread ID — used for history retrieval |
| `channelId` | String | No | Channel identifier (e.g. `msteams`) |
| `serviceUrl` | String | No | Teams service URL — not used by MVP |
| `replyToId` | String | No | Message reply context — not used |

### Response DTO — `TeamsActivityResponse`

`com.bytehr.api.dto.TeamsActivityResponse`

```json
{
  "type": "message",
  "text": "You are entitled to **20 working days** of paid annual vacation...\n\n---\n**Sources:**\n- **vacation-policy.md** — [View document](/path/to/file)\n",
  "textFormat": "markdown"
}
```

| Field | Type | Description |
|---|---|---|
| `type` | String | Always `"message"` for chat replies; `"invoke"` for non-message ACK |
| `text` | String | Markdown-formatted answer with appended **Sources** section |
| `textFormat` | String | Always `"markdown"` |

### Processing Flow

```
POST /api/messages (TeamsActivity)
        │
        ▼
TeamsController.handleActivity()
  ├─ type ≠ "message"  →  200 OK  {type:"invoke", text:""}
  ├─ text is blank     →  200 OK  "Hello! I am ByteHR AI..."
  └─ text present      →
        │
        ├─ extractUserId()      aadObjectId → userId
        ├─ resolveCountry()     → null (MVP: no Graph call)
        ├─ ConversationService.getConversationHistory(conversationId, 6)
        │
        ▼
    HrResponseAgent.answer(question, country, conversationId, userId, userName, history)
        │
        ├─ LanguageDetectionService.detectLanguage(question)  → "en"/"sq"/"sr"
        ├─ VectorSearchService.search(question, country, topK=5)
        │     ├─ EmbeddingService.generateEmbedding(question)  → Ollama /api/embeddings
        │     ├─ pgvector cosine similarity search (country filter + fallback to global)
        │     └─ Filter by minScore=0.5
        │
        ├─ maxScore < 0.6  →  "I could not find a reliable answer. Please contact HR."
        │
        ├─ Build system prompt (inject document chunks as context)
        ├─ Build message list (system + history + question)
        ├─ ChatService.generateAnswer(messages)  → Ollama /api/chat (qwen3:8b)
        ├─ ConversationService.saveConversation(...)
        └─ AnalyticsRepository.save(...)
        │
        ▼
    HrChatResponse { answer, citations, confidenceScore, detectedLanguage, answered }
        │
        ▼
TeamsController.formatReply()
  Appends "---\n**Sources:**\n- **docName** — [link](path)\n"
        │
        ▼
TeamsActivityResponse { type:"message", text: markdown, textFormat:"markdown" }
```

### Internal Response Model — `HrChatResponse`

`com.bytehr.api.dto.HrChatResponse` (internal — not returned to caller directly)

| Field | Type | Description |
|---|---|---|
| `answer` | String | LLM-generated answer text |
| `citations` | `List<Citation>` | Source documents used |
| `confidenceScore` | double | Max cosine similarity score (0–1) |
| `detectedLanguage` | String | BCP-47 code: `"en"`, `"sq"`, `"sr"` |
| `answered` | boolean | `false` if low-confidence fallback was used |

### Citation Model — `Citation`

`com.bytehr.api.dto.Citation`

| Field | Type | Description |
|---|---|---|
| `documentName` | String | Filename of the source document |
| `sourcePath` | String | Absolute filesystem path or SharePoint URL |
| `section` | String | Section heading (not populated — reserved) |
| `pageNumber` | Integer | Page number if available (PDFs) |
| `webUrl` | String | SharePoint web URL (not populated in local mode) |

---

## Endpoint 2 — Document Synchronisation

### Identification

| Attribute | Value |
|---|---|
| **Type** | Admin trigger |
| **URL** | `POST /api/sync` |
| **Controller** | `com.bytehr.api.SyncController` |
| **Authentication** | `permitAll()` |
| **Content-Type** | No request body |

### Request

No request body. No parameters.

```bash
curl -X POST http://localhost:8080/api/sync
```

### Response DTO — `SyncResponse`

`com.bytehr.api.dto.SyncResponse`

```json
{
  "status": "success",
  "documentsProcessed": 12,
  "totalDocumentsIndexed": 12,
  "durationMs": 45230,
  "sourceType": "local",
  "message": "12 document(s) processed. 12 total indexed."
}
```

| Field | Type | Description |
|---|---|---|
| `status` | String | `"success"` or `"error"` |
| `documentsProcessed` | int | New or updated documents indexed in this run |
| `totalDocumentsIndexed` | long | Total documents in database after sync |
| `durationMs` | long | Wall-clock time of the sync in milliseconds |
| `sourceType` | String | `"local"` or `"sharepoint"` |
| `message` | String | Human-readable summary or error message |

### Processing Flow

```
POST /api/sync
        │
        ▼
SyncController.triggerSync()
  └─ documentSyncService.synchronize()
        │
  [if source.type=local]
  LocalDocumentSourceServiceImpl
    ├─ Walk sample-data/hr-documents/ recursively
    ├─ For each .md/.txt/.pdf/.docx:
    │     ├─ Check sharepointItemId="local://path" in DB
    │     ├─ Compare lastModified → skip if unchanged
    │     └─ DocumentProcessorService.processDocument(doc, bytes)
    │           ├─ Tika.parseToString()
    │           ├─ LanguageDetectionService.detectLanguage()
    │           ├─ ChunkingService.chunk(1000 chars, 200 overlap)
    │           ├─ EmbeddingService.generateEmbedding() per chunk
    │           └─ JdbcTemplate UPDATE embedding = ?::vector
    └─ return count
        │
  [if source.type=sharepoint]
  DocumentSyncServiceImpl
    └─ SharePointClient.listDocuments() → download → same pipeline
        │
        ▼
  DocumentRepository.count() → totalDocumentsIndexed
        │
        ▼
SyncResponse { status, documentsProcessed, totalDocumentsIndexed, durationMs, sourceType }
```

---

## Endpoint 3 — Health Check

### Identification

| Attribute | Value |
|---|---|
| **URL** | `GET /actuator/health` |
| **Type** | Spring Boot Actuator |
| **Authentication** | `permitAll()` |

### Response

```json
{ "status": "UP" }
```

Used by Docker Compose health check:
```yaml
test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
```

---

## Endpoint 4 — Application Info

| Attribute | Value |
|---|---|
| **URL** | `GET /actuator/info` |
| **Type** | Spring Boot Actuator |
| **Authentication** | `permitAll()` |

Returns empty `{}` — no custom `info.*` properties are configured.

---

## Key Finding: No Dedicated Internal Chat Endpoint

### Observation

There is **no `POST /api/chat` endpoint** in the codebase.

The RAG question-answering pipeline (`HrResponseAgent`) is **only accessible through the Teams webhook** (`POST /api/messages`). There is no dedicated endpoint that accepts a plain `{"question": "...", "country": "AL"}` payload.

### Impact

| Consequence | Detail |
|---|---|
| **Testing complexity** | Every test call must include the full Bot Framework Activity JSON structure |
| **Demo friction** | `curl` demos require the complete `TeamsActivity` shape |
| **No direct API access** | External systems cannot call the RAG pipeline without mimicking Teams |
| **Country filtering untestable** | `resolveCountry()` always returns `null` in the MVP — no way to override |

### Request format required for current testing

```bash
curl -s -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "type": "message",
    "text": "How many vacation days do I have?",
    "from": {"id": "test-user", "name": "Test Employee"},
    "conversation": {"id": "test-conv-1"}
  }'
```

### Recommendation

Add a `POST /api/chat` endpoint that accepts a direct `HrChatRequest`:

```json
{
  "question": "How many vacation days do I have?",
  "country": "AL",
  "userId": "optional-user-id",
  "conversationId": "optional-conv-id"
}
```

And returns `HrChatResponse` directly (with `answer`, `citations`, `confidenceScore`).  
This would make the RAG pipeline independently testable and useful for demos.

---

## Additional Finding: application.yml logging misconfiguration

### Observation

The `logging.level` properties are mis-indented under the `bytehr:` key instead of under a top-level `logging:` key:

```yaml
# Current (incorrect)
bytehr:
  source:
    type: local
    ...
    sync-interval-ms: 3600000

  level:               ← nested under bytehr: — Spring Boot ignores this
    com.bytehr: INFO
    org.hibernate.SQL: WARN
```

```yaml
# Correct form
bytehr:
  source: ...

logging:             ← top-level key
  level:
    com.bytehr: INFO
    org.hibernate.SQL: WARN
```

### Impact

The configured log levels (`com.bytehr: INFO`, `org.hibernate.SQL: WARN`) are silently ignored.  
Spring Boot uses its default log levels instead.  
This is a configuration-only defect — no code changes required to fix.

---

## RAG Pipeline — Service Dependency Map

```
POST /api/messages
  └── TeamsController
        └── HrResponseAgent (HrResponseAgentImpl)
              ├── LanguageDetectionService
              │     └── Apache Tika Optimaize detector
              ├── VectorSearchService (VectorSearchServiceImpl)
              │     ├── EmbeddingService (EmbeddingServiceImpl)
              │     │     └── OllamaClient  →  POST http://ollama:11434/api/embeddings
              │     │                          model: nomic-embed-text  (768-dim)
              │     └── JdbcTemplate
              │           SQL: SELECT ... FROM document_chunks
              │                ORDER BY embedding <=> ?::vector  LIMIT 5
              ├── ChatService (ChatServiceImpl)
              │     └── OllamaClient  →  POST http://ollama:11434/api/chat
              │                          model: qwen3:8b
              ├── ConversationService (ConversationServiceImpl)
              │     └── ConversationRepository (JPA)
              └── AnalyticsRepository (JPA)
```

---

## Configuration Parameters Affecting Endpoints

| Parameter | Default | Endpoint Affected | Effect |
|---|---|---|---|
| `vector-search.top-k` | `5` | `/api/messages` | Max document chunks retrieved per query |
| `vector-search.min-score` | `0.5` | `/api/messages` | Minimum cosine similarity to include a chunk |
| `vector-search.confidence-threshold` | `0.6` | `/api/messages` | Below this max score → low-confidence fallback |
| `ollama.timeout-seconds` | `60` | `/api/messages` | Timeout for both embedding and chat LLM calls |
| `bytehr.source.type` | `local` | `/api/sync` | Activates local or SharePoint sync service |
| `bytehr.source.local-path` | `./sample-data/hr-documents` | `/api/sync` | Root path for local document scanning |

---

## Summary

| Endpoint | Classification | RAG Involved | Status |
|---|---|---|---|
| `POST /api/messages` | Teams Bot webhook | ✅ Yes — full pipeline | Active, functional |
| `POST /api/sync` | Admin / operations | ❌ No — ingestion only | Active, functional |
| `GET /actuator/health` | Infrastructure | ❌ No | Active, functional |
| `POST /api/chat` | Direct chat API | N/A — **does not exist** | ⚠️ Gap — recommended addition |
