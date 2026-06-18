# PDF Document Drop Folder — Albania

Place Albanian HR PDF documents in this folder.

## Country mapping

Folder name `albania-docs` → Country code `AL`

All documents here will be tagged `country=AL` and prioritised for Albanian employee queries.

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
3. ByteHR AI will index all documents and answer Albanian employee questions

## Sample documents already available

The sibling `albania/` folder contains English and Albanian markdown sample documents for demo purposes. These are scanned automatically alongside this folder.

## Naming suggestion

```
HR-Handbook-Albania-2026.pdf
Vacation-Policy-Albania.pdf
Sick-Leave-Policy-Albania.pdf
Benefits-Guide-Albania.pdf
```
