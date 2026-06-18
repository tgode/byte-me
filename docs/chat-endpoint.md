# ByteHR AI — Direct Chat Endpoint

**Endpoint:** `POST /api/chat`  
**Purpose:** Direct RAG question-answering for MVP testing and hackathon demos  
**Added in:** MVP release

---

## Overview

The `/api/chat` endpoint provides direct access to the ByteHR RAG pipeline without requiring the Microsoft Teams Bot Framework Activity format. It is the recommended way to test and demonstrate the system during local development and hackathon presentations.

The endpoint invokes **exactly the same pipeline** as the Teams webhook (`POST /api/messages`):

```
POST /api/chat
      │
      ▼  (identical to Teams webhook from this point)
HrResponseAgent
  ├── LanguageDetectionService  (Tika Optimaize)
  ├── VectorSearchService       (pgvector cosine similarity)
  │     └── EmbeddingService    (Ollama nomic-embed-text)
  ├── ChatService               (Ollama qwen3:8b)
  ├── ConversationService       (multi-turn history)
  └── AnalyticsRepository
```

---

## Request

**URL:** `POST http://localhost:8080/api/chat`  
**Content-Type:** `application/json`

### Request Body — `ChatRequest`

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `question` | String | **Yes** | 1–2000 chars, not blank | The HR question to answer |
| `country` | String | No | `AL` or `RS` only | Country code for policy filtering |
| `sessionId` | String | No | Max 128 chars | Reuse across requests for multi-turn context |

### Minimal request

```json
{
  "question": "How many vacation days do employees receive?"
}
```

### Full request

```json
{
  "question": "How many vacation days do employees receive?",
  "country": "AL",
  "sessionId": "demo-session-001"
}
```

---

## Response

### Response Body — `ChatResponse`

| Field | Type | Description |
|---|---|---|
| `answer` | String | LLM-generated HR answer |
| `citations` | Array | Source documents used (may be empty on low confidence) |
| `citations[].document` | String | Source document filename |
| `citations[].section` | String | Section heading (reserved, may be null) |
| `citations[].sourcePath` | String | File path or SharePoint URL |
| `citations[].pageNumber` | Integer | Page number within document (PDFs) |
| `confidence` | Double | Max cosine similarity score (0.0–1.0) |
| `detectedLanguage` | String | BCP-47 code of detected question language |
| `answered` | Boolean | `false` if low-confidence fallback was triggered |

### Successful answer

```json
{
  "answer": "You are entitled to **20 working days** of paid annual vacation per calendar year, as required by the Albanian Labour Code. Employees with more than 5 years of service receive 25 working days.",
  "citations": [
    {
      "document": "vacation-policy.md",
      "section": null,
      "sourcePath": "/app/sample-data/hr-documents/albania/vacation-policy.md",
      "pageNumber": null
    }
  ],
  "confidence": 0.92,
  "detectedLanguage": "en",
  "answered": true
}
```

### Low-confidence fallback (question not answerable from documents)

```json
{
  "answer": "I could not find a reliable answer. Please contact HR.",
  "citations": [],
  "confidence": 0.0,
  "detectedLanguage": "en",
  "answered": false
}
```

---

## HTTP Status Codes

| Code | Condition |
|---|---|
| `200 OK` | Request processed successfully (check `answered` field for result quality) |
| `400 Bad Request` | Validation failed: blank question, invalid country code, question too long |
| `500 Internal Server Error` | Ollama unreachable, database unavailable |

---

## Validation Rules

| Rule | Detail |
|---|---|
| `question` required | Returns 400 if missing or blank |
| `question` max length | Returns 400 if longer than 2000 characters |
| `country` allowed values | `AL` (Albania) or `RS` (Serbia) only; returns 400 for any other value |
| `sessionId` max length | Returns 400 if longer than 128 characters |

---

## curl Examples

### Basic HR question

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How many vacation days do I have?"}' \
  | python3 -m json.tool
```

### Country-specific query (Albania)

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How many vacation days do I have?",
    "country": "AL"
  }' | python3 -m json.tool
```

### Albanian language query

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Sa dite pushimi kam?",
    "country": "AL"
  }'
```

### Serbian language query

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Koliko dana godisnjeg odmora imam?",
    "country": "RS"
  }'
```

### Multi-turn conversation (session)

```bash
SESSION="demo-$(date +%s)"

# Turn 1
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"How many vacation days do I have?\", \"sessionId\": \"$SESSION\"}"

# Turn 2 — follow-up uses conversation history
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"Can I carry them over to next year?\", \"sessionId\": \"$SESSION\"}"
```

### Off-topic rejection test

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the capital of France?"}'
# Expected: answered=false or out-of-scope message
```

---

## Swagger UI

When the application is running, the interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8080/v3/api-docs
```

---

## Demo Workflow

Run these commands in order for a complete hackathon demo:

```bash
# 1. Ensure documents are indexed
curl -X POST http://localhost:8080/api/sync

# 2. English question
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How many vacation days do employees in Albania get?", "country": "AL"}' \
  | python3 -m json.tool

# 3. Same question in Albanian
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Sa dite pushimi kam ne vit?", "country": "AL"}' \
  | python3 -m json.tool

# 4. Serbian
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Koliko dana godisnjeg odmora imam?", "country": "RS"}' \
  | python3 -m json.tool

# 5. Benefits query
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What health insurance does the company provide?"}' \
  | python3 -m json.tool

# 6. Remote work
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Can I work from home?"}' \
  | python3 -m json.tool

# 7. Off-topic rejection
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the weather in Tirana?"}' \
  | python3 -m json.tool

# 8. Multi-turn follow-up
SESSION="hackathon-demo"
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"How many vacation days do I have?\", \"sessionId\": \"$SESSION\"}"

curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"What happens if I don't use them all?\", \"sessionId\": \"$SESSION\"}"
```

---

## Relationship to Teams Endpoint

| | `POST /api/chat` | `POST /api/messages` |
|---|---|---|
| **Format** | Plain JSON | Bot Framework Activity |
| **Use case** | Testing, demos, curl | Microsoft Teams bot |
| **RAG pipeline** | Identical | Identical |
| **Conversation history** | Via `sessionId` field | Via Teams conversation ID |
| **Country detection** | Via `country` field | Via Teams user AAD profile |
| **Citations in response** | Structured JSON array | Embedded in markdown text |
| **Authentication** | None (MVP) | Bot Framework JWT (production) |

---

## Unit Tests

Test coverage for `ChatControllerTest`:

| Test | Scenario | Expected |
|---|---|---|
| `chat_validQuestion_returns200WithAnswer` | Happy path with country | 200, answer + citations |
| `chat_noCountry_returns200` | No country field | 200, global search |
| `chat_withSessionId_usesProvidedSession` | Session reuse for multi-turn | 200, history retrieved |
| `chat_lowConfidence_returns200WithFallbackMessage` | Unanswerable question | 200, `answered=false` |
| `chat_multipleCitations_allMapped` | Multiple source documents | 200, all citations mapped |
| `chat_blankQuestion_returns400` | Empty string question | 400 |
| `chat_nullQuestion_returns400` | Missing question field | 400 |
| `chat_invalidCountryCode_returns400` | `country="INVALID"` | 400 |
| `chat_questionTooLong_returns400` | 2001-character question | 400 |
| `chat_emptyBody_returns400` | `{}` body | 400 |

Run: `mvn test -Dtest=ChatControllerTest`
