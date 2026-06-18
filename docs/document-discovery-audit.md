# ByteHR AI — Document Discovery Audit

**Date:** 2026-06-18  
**Problem:** `documentsProcessed=18` despite ~60 PDF documents on disk  
**Status:** Root causes identified and fixed

---

## Root Cause Analysis

### Cause 1 — Wrong source path (primary cause)

The application was configured to scan `./sample-data/hr-documents` which contained only
**18 markdown sample files** created during development. The real PDFs were placed in
`./docs/sample-data/hr-documents/`. This path was never scanned.

| Config | Before (wrong) | After (fixed) |
|---|---|---|
| `application.yml` default | `./sample-data/hr-documents` | `./docs/sample-data/hr-documents` |
| `docker-compose.yml` env | `/app/sample-data/hr-documents` | `/app/docs/sample-data/hr-documents` |
| Docker volume mount | `./sample-data:/app/sample-data:ro` | `./docs/sample-data:/app/docs/sample-data:ro` |
| `.env.example` | `sample-data/hr-documents` | `docs/sample-data/hr-documents` |

### Cause 2 — Windows Zone.Identifier artifacts (secondary cause)

The PDF folder contained 98 Windows NTFS Alternate Data Stream (ADS) metadata files.
These appear on Linux as files with `:` in their name, e.g.:
```
EPR Introduction Eng Albania.pdf:Zone.Identifier
EPR Introduction Eng Albania.pdf:Zone.Identifier:Zone.Identifier
```

These were already filtered by the extension check (`Identifier` ∉ supported set), but
caused confusion in file counts. Explicit `:` detection was added to log them clearly.

---

## Actual File Inventory

After fix: **49 real documents** discovered across 2 country folders.

### albania-docs/ (country=AL) — 26 files

| File | Type |
|---|---|
| 2025 EPR Final Review_ Deck for Webinar Manager_Update & Feedback_Feb 2026_ENGVersion.pdf | PDF |
| 2025 Final Review_Workday user manual 1.pdf | PDF |
| 2025 Final Review_Workday user manual.pdf | PDF |
| 2025 Mid Year Check - In_Workday user manual.pdf | PDF |
| 2025 Mid Year Check - In_Workday user manual.pptx | PPTX |
| 2025_EPR GLOBAL_Eng_July 2025.pdf | PDF |
| 2025_EPR GLOBAL_Eng_July 2025.pptx | PPTX |
| 2025_Final_Review_FAQ_English.pdf | PDF |
| 2026 EPR Goal Setting_User Manual.pdf | PDF |
| 2026_EPR GLOBAL_Eng_Mar 2026_for Global.pdf | PDF |
| EPR Introduction Eng Albania.pdf | PDF |
| Engineering Albania - Public Holidays list 2026.pdf | PDF |
| Festat Zyrtare 2025.pdf | PDF |
| Festat Zyrtare 2026.pdf | PDF |
| How to Access LinkedIn Learning via Workday.pdf | PDF |
| Kodi i Etikës - Engineering Group.pdf | PDF |
| Manual i sigurise dhe shendetit ne pune - DVR.pdf | PDF |
| PIP - Engineering Albania 07.10.pdf | PDF |
| POLITIKA KUNDËR KORRUPSIONIT - Engineering Group.pdf | PDF |
| POLITIKA KUNDËR KORRUPSIONIT- Shtojca 1 - Engineering Group.pdf | PDF |
| POLITIKA KUNDËR KORRUPSIONIT- Shtojca 2 - Engineering Group.pdf | PDF |
| Policy Whistleblowing 28022025.pdf | PDF |
| Politika e referimit .pdf | PDF |
| Rregullore e brendshme - Engineering Albania.pdf | PDF |
| WC EAL_Internal_Procedures__Processes - October 2025.pdf | PDF |
| ZTP ENG.pdf | PDF |

### sebia-docs/ (country=RS) — 23 files

| File | Type |
|---|---|
| DG 2026 - Uputstvo za registraciju na portal za klijente - zdravstveno PZO.pdf | PDF |
| Delta Generali 2026 - Tabela Pokrica.pdf | PDF |
| Delta Generali 2026 - Cene prijave clanova porodice.pdf | PDF |
| Delta Gnerali 2026 - Uputstvo za koriscenje osiguranja.pdf | PDF |
| ENG Internet BIGZ connection procedure 161225.pdf | PDF |
| ENG23_manifesto_community-DEI_EN_VER (1).pdf | PDF |
| FitPass - Prezentacija za zaposlene.pdf | PDF |
| Laptop Replacement Policy Guideline Nearshore.pdf | PDF |
| Meeting room reservation procedure Eng Serbia.pdf | PDF |
| OPCIJE I SUGESTIJE ZA DOLAZAK DO NOVOOTVORENIH KANCELARIJA ENG SERBIA.pdf | PDF |
| Office attendance and house rules Eng Serbia 110226.pdf | PDF |
| PLANET SYSTEM FOR DESK RESERVATION - How-to Manual Eng Serbia.pdf | PDF |
| Procedure - Cesto postavljena pitanja - Frequently Asked Questions.pdf | PDF |
| Rulebooks - Data Breach Policy - Groupwide.pdf | PDF |
| Rulebooks - Pravilnik o Postupku unutrašnjeg uzbunjivanja.pdf | PDF |
| Rulebooks - Pravilnik o poslovnoj tajni - Confidential Information rulebook.pdf | PDF |
| Rulebooks - Pravilnik o sprecavanju zlostavljanja na radu.pdf | PDF |
| Rulebooks - Pravilnik o zastiti podataka o licnosti.pdf | PDF |
| Workday - FAQs.pdf | PDF |
| Zahtev za privremeno trazenje lokalnih dozvola.pdf | PDF |
| Zakon-o-rodnoj-ravnopravnosti.pdf | PDF |
| [ENG] Internal Policies and Procedures - ESL.pdf | PDF |
| [SRB] Internal Policies and Procedures - ESL1.pdf | PDF |

---

## Discovery Log Format

After the fix, every startup and sync emits structured logs:

```
[Document Discovery] rootPath=/app/docs/sample-data/hr-documents
[Document Discovery] file=/app/docs/sample-data/hr-documents/albania-docs/ZTP ENG.pdf
[Document Discovery] file=/app/docs/sample-data/hr-documents/albania-docs/Festat Zyrtare 2026.pdf
...
[Document Discovery] filesDiscovered=49 filesProcessed=49 filesSkipped=0
```

Startup banner:
```
╔══════════════════════════════════════════════════════╗
║              ByteHR AI  —  Startup                  ║
╠══════════════════════════════════════════════════════╣
║  Source Type  : LOCAL                                ║
║  Source Path  : ./docs/sample-data/hr-documents      ║
║  Abs Path     : /app/docs/sample-data/hr-documents   ║
║  Path Exists  : YES                                  ║
║  Files Found  : 49                                   ║
║  Indexed Docs : 0                                    ║
╚══════════════════════════════════════════════════════╝
```

---

## Zone.Identifier Artifacts

The `docs/sample-data/hr-documents/` folder contains 98 Windows NTFS ADS files:

```
Festat Zyrtare 2025.pdf:Zone.Identifier
Festat Zyrtare 2025.pdf:Zone.Identifier:Zone.Identifier
```

These are created by Windows when files are downloaded from the internet (security zones).
They appear as separate files on Linux due to NTFS ADS handling in WSL/copy operations.

**Handling:**
- `LocalDocumentSourceServiceImpl` now explicitly detects filenames containing `:`
- These are logged at DEBUG level and silently skipped
- They do NOT appear in `filesDiscovered` count
- They were already filtered by extension (`Identifier` ∉ supported set) — the explicit filter adds clarity

---

## Verification

After reindex:

```bash
# Step 1: trigger reindex
curl -s -X POST http://localhost:8080/api/admin/reindex | python3 -m json.tool
# Expected: documentsProcessed=49 (or more if markdown sample docs also scanned)

# Step 2: verify file count in database
docker exec bytehr-postgres psql -U bytehr -d bytehr -c \
  "SELECT country, COUNT(*) FROM documents GROUP BY country ORDER BY country;"

# Expected:
#  country | count
# ---------+-------
#  AL      |    26
#  RS      |    23
```

---

## Configuration Reference

### Local development (without Docker)

```yaml
# application.yml — already updated
bytehr:
  source:
    local-path: ./docs/sample-data/hr-documents
```

### Docker Compose

```yaml
# docker-compose.yml — already updated
services:
  bytehr-api:
    environment:
      BYTEHR_LOCAL_PATH: /app/docs/sample-data/hr-documents
    volumes:
      - ./docs/sample-data:/app/docs/sample-data:ro
```

### Override via environment variable

```bash
# .env
BYTEHR_LOCAL_PATH=/custom/path/to/hr-documents
```

---

## Supported File Extensions

| Extension | Format | Status |
|---|---|---|
| `.pdf` | PDF | ✅ Primary format |
| `.docx` | Word | ✅ Supported |
| `.pptx` | PowerPoint | ✅ Added (2 PPTX files in corpus) |
| `.txt` | Plain text | ✅ Supported |
| `.md` | Markdown | ✅ Supported (sample docs) |
