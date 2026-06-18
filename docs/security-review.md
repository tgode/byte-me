# ByteHR AI — Security Review

**Date:** 2026-06-18  
**Scope:** Document ingestion, RAG pipeline, API responses, citation handling

---

## Security Controls Summary

| Control | Status | Implementation |
|---|---|---|
| PII redaction before indexing | ✅ Active | `RedactionServiceImpl` |
| No raw document exposure | ✅ Enforced | Only chunk excerpts in context |
| No filesystem path in API | ✅ Enforced | `@JsonIgnore` on `Citation.sourcePath` |
| No path in Teams messages | ✅ Enforced | `webUrl`-only links in `TeamsController` |
| No path in chat API | ✅ Enforced | `sourcePath` not mapped in `ChatController` |
| Strict RAG mode | ✅ Active | `bytehr.rag.strict-mode=true` |
| Temperature = 0.0 | ✅ Active | Deterministic output, no creative invention |

---

## 1. Raw Document Exposure

### Requirement
Never return full documents, entire PDF text, or raw file content.

### Implementation
The RAG pipeline returns only **chunk excerpts** (max 1500 characters total) as context to the LLM. The LLM is instructed to generate a concise answer from this context — it does not receive the full document.

**Verified in `HrResponseAgentImpl.buildContext()`:**
```java
private String buildContext(List<RelevantChunk> chunks, int maxContextChars) {
    // ...
    if (content.length() > remaining) {
        content = content.substring(0, cutAt) + "...";  // truncated
    }
    // ...
}
```

The final answer returned to users is the LLM-generated text, not raw document content.

---

## 2. Filesystem Path Leakage

### Requirement
Never return filesystem paths such as `/app/sample-data/hr-documents/albania-docs/file.pdf`.

### Implementation — Three layers of defence

**Layer 1: `Citation.sourcePath` marked `@JsonIgnore`**
```java
@JsonIgnore
private String sourcePath;  // never serialized in API responses
```

**Layer 2: `ChatController.mapCitations()` does not map sourcePath**
```java
.map(c -> ChatCitation.builder()
        .document(c.getDocumentName())
        .section(c.getSection())
        // sourcePath intentionally NOT mapped
        .pageNumber(c.getPageNumber())
        .build())
```

**Layer 3: `TeamsController.formatReply()` only links via `webUrl`**
```java
// Only show link for SharePoint documents — never filesystem paths
if (citation.getWebUrl() != null && !citation.getWebUrl().isBlank()) {
    sb.append(" — [View document](").append(citation.getWebUrl()).append(")");
}
```

**Correct citation format:**
```json
{
  "document": "vacation-policy.md",
  "pageNumber": 2
}
```

**Forbidden:**
```json
{
  "sourcePath": "/app/sample-data/hr-documents/albania-docs/vacation-policy.pdf"
}
```

---

## 3. PII in Indexed Content

### Requirement
Personally identifiable information (PII) must not enter the vector database.

### Implementation — `RedactionServiceImpl`

Applied **before** chunking and embedding:

```
Document text → RedactionService.redact() → ChunkingService → EmbeddingService → pgvector
```

**Patterns redacted:**

| Category | Pattern | Replacement |
|---|---|---|
| Email | `user@domain.com` | `[EMAIL REDACTED]` |
| Phone | `+355 4 123 456` | `[PHONE REDACTED]` |
| IBAN | `AL35XXXX...` | `[IBAN REDACTED]` |
| Albanian NID | `A12345678B` | `[ID REDACTED]` |
| Albanian NIPT | `K12345678L` | `[ID REDACTED]` |
| Serbian JMBG | `1234567890123` | `[ID REDACTED]` |

**Configuration:** `bytehr.security.redaction.enabled=true` (default, cannot be bypassed by users)

**Verification query:**
```sql
-- Verify no emails in indexed content
SELECT COUNT(*) FROM document_chunks
WHERE content ~ '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}';
-- Expected: 0

-- Verify no IBANs
SELECT COUNT(*) FROM document_chunks
WHERE content ~ '[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}';
-- Expected: 0
```

---

## 4. Answer Content Security

### Requirement
The LLM must only answer from retrieved document context — no hallucinated personal data.

### Implementation — Strict RAG Mode

System prompt (`SYSTEM_PROMPT_STRICT`) explicitly forbids:
- Contact details not in the document
- Email addresses not in the document
- Personal information about employees
- Any information from general knowledge

```
3. Do NOT add HR contact details, email addresses, phone numbers, or URLs unless
   they appear word-for-word in the context.
...
CRITICAL: Use ONLY the above context. Do not supplement with general knowledge.
```

Additionally, `temperature=0.0` prevents creative invention — the model produces deterministic output based solely on the retrieved context.

---

## 5. Personal Employee Information

### Requirement
Never return: full documents, personal employee information, salaries, signatures, IDs, email addresses, phone numbers.

### Controls

| Data type | Control |
|---|---|
| Full documents | Never stored in DB as-is; only chunks are stored |
| Salary data | PII redaction handles numeric patterns; strict prompt forbids fabrication |
| Signatures | PDFs with signatures: signature images are not extracted by Tika text parser |
| IDs (NID/JMBG) | Redacted before indexing |
| Email addresses | Redacted before indexing |
| Phone numbers | Redacted before indexing |

---

## 6. Admin Endpoint Security

### `POST /api/admin/reindex`

**Current state:** Permitted without authentication (suitable for demo/local)  
**Production recommendation:** Secure with Basic Auth or IP whitelist:

```java
// SecurityConfig — production hardening
.requestMatchers(new AntPathRequestMatcher("/api/admin/**"))
    .hasRole("ADMIN")  // require admin role
```

Or restrict via Docker network:
```yaml
# docker-compose.yml — expose admin port on localhost only
ports:
  - "127.0.0.1:8080:8080"
```

---

## 7. SharePoint Integration (Production)

When `BYTEHR_SOURCE_TYPE=sharepoint`:
- Azure credentials are read from environment variables — never hardcoded
- `SHAREPOINT_CLIENT_SECRET` should use Azure Key Vault in production
- SharePoint URLs are valid public URLs and can appear in citations

---

## Known Limitations

| Limitation | Risk | Mitigation |
|---|---|---|
| Redaction regex not exhaustive | Some PII formats may slip through | Review patterns for specific document types |
| JMBG (13-digit) may false-match | Long product codes or order numbers | Log and review redaction hits |
| Tika page count estimation | Approximate page numbers in citations | Accurate enough for citation guidance |
| Admin endpoint unauthenticated | Reindex available to anyone on network | Restrict in production (see §6) |
