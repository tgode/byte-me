# PDF Document Drop Folder — Serbia

Place Serbian HR PDF documents in this folder.

## Country mapping

Folder name `sebia-docs` → Country code `RS`

(Note: `sebia-docs` is the configured folder name; both `sebia` and `serbia` are recognised by the country detection logic.)

All documents here will be tagged `country=RS` and prioritised for Serbian employee queries.

## Supported formats

| Format | Extension | Notes |
|---|---|---|
| PDF | `.pdf` | **Primary format** — text extraction + page citations |
| Word | `.docx` | Supported |
| Plain text | `.txt` | Supported |
| Markdown | `.md` | Supported (included in sample data) |

## How to use

1. Drop PDF files into this folder
2. Run: `curl -X POST http://localhost:8080/api/admin/reindex`
3. ByteHR AI will index all documents and answer Serbian employee questions

## Naming suggestion

```
HR-Handbook-Serbia-2026.pdf
Vacation-Policy-Serbia.pdf
Sick-Leave-Policy-Serbia.pdf
Benefits-Guide-Serbia.pdf
```
