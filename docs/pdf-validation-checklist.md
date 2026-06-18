# ByteHR AI — PDF Validation Checklist

**Date:** 2026-06-18  
**Purpose:** Pre-deployment validation of the document ingestion and RAG pipeline

---

## Pre-Deployment Checklist

### Infrastructure

- [ ] PostgreSQL is running with pgvector extension installed
  ```sql
  SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
  ```
- [ ] Ollama is running and models are installed
  ```bash
  curl http://localhost:11434/api/tags | python3 -m json.tool
  # Expected: nomic-embed-text, qwen3:1.7b
  ```
- [ ] Application starts successfully
  ```bash
  curl http://localhost:8080/actuator/health
  # Expected: {"status":"UP"}
  ```

---

### Document Ingestion

- [ ] `POST /api/admin/reindex` completes without error
  ```bash
  curl -s -X POST http://localhost:8080/api/admin/reindex | python3 -m json.tool
  # Expected: status=success, documentsProcessed>0, embeddingsCreated>0
  ```
- [ ] All documents are fully embedded
  ```sql
  SELECT COUNT(*) = COUNT(embedding) AS all_embedded FROM document_chunks;
  -- Expected: true (t)
  ```
- [ ] Country detection is correct
  ```sql
  SELECT name, country FROM documents ORDER BY country, name;
  -- albania/ and albania-docs/ → AL
  -- serbia/ and sebia-docs/ → RS
  -- global/ → NULL
  ```

---

### Validation Questions

#### Question 1 — English, Albania policy

**Question:** `"What vacation entitlement do employees receive in Albania?"`  
**Country:** `AL`

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"What vacation entitlement do employees receive in Albania?","country":"AL"}'
```

**Expected:**
- [ ] `answered: true`
- [ ] `confidence > 0.35`
- [ ] Answer mentions **20 working days** (less than 5 years) or **25 working days** (5+ years)
- [ ] Citation references an Albanian vacation policy document

---

#### Question 2 — Serbian, Serbia policy

**Question:** `"Koliko dana godišnjeg odmora imam?"`  
**Country:** `RS`

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"Koliko dana godisnjeg odmora imam?","country":"RS"}'
```

**Expected:**
- [ ] `answered: true`
- [ ] `detectedLanguage: "sr"` (or `"en"` if detection fails on short query)
- [ ] Answer contains number of vacation days for Serbia (20–25)
- [ ] Answer in Serbian (Srpski) — "Imate pravo na X radnih dana"
- [ ] Citation references Serbian vacation policy document

---

#### Question 3 — Albanian, Albania policy

**Question:** `"Sa ditë pushimi vjetor kam?"`  
**Country:** `AL`

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"Sa dite pushimi vjetor kam?","country":"AL"}'
```

**Expected:**
- [ ] `answered: true`
- [ ] `detectedLanguage: "sq"` (or `"en"` if short query)
- [ ] Answer contains **20** or **25 ditë pune** (working days in Albanian)
- [ ] Answer in Albanian (Shqip)
- [ ] Citation references Albanian vacation policy document

---

### Security Checks

- [ ] API response citations do NOT contain filesystem paths
  ```bash
  curl -s -X POST http://localhost:8080/api/chat \
    -H "Content-Type: application/json" \
    -d '{"question":"vacation days","country":"AL"}' \
  | python3 -c "
  import json,sys
  r=json.load(sys.stdin)
  for c in r.get('citations',[]):
      assert '/app/' not in str(c), f'Path leaked: {c}'
      assert '/home/' not in str(c), f'Path leaked: {c}'
      assert 'sample-data' not in str(c), f'Path leaked: {c}'
  print('✅ No path leakage detected')
  "
  ```
- [ ] Citations show only document name and page number (no full paths)
- [ ] Redaction removes email addresses from indexed content
  ```sql
  SELECT COUNT(*) FROM document_chunks
  WHERE content ~ '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}';
  -- Expected: 0 (all emails redacted before indexing)
  ```

---

### Multi-Language Consistency

Run all three queries and verify:
- [ ] EN, SQ, SR all return `answered: true`
- [ ] Each returns citations from the correct language/country document
- [ ] No cross-language answer bleeding (Albanian question → Albanian answer)

---

### Reindex Endpoint

- [ ] `POST /api/admin/reindex` response contains all three fields:
  ```json
  {
    "documentsProcessed": 16,
    "chunksCreated": 48,
    "embeddingsCreated": 48
  }
  ```
- [ ] After reindex, `chunksCreated == embeddingsCreated` (no missing embeddings)
- [ ] Re-running sync (`POST /api/sync`) after reindex returns `documentsProcessed: 0` (no changes detected)
