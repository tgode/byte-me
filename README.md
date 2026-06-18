# ByteHR AI

An AI-powered HR Assistant integrated into Microsoft Teams, answering HR questions for employees in Albania and Serbia using official company documentation from SharePoint.

## Architecture

```
Microsoft Teams
      │
      ▼
ByteHR Teams Bot  ──►  Spring Boot API  ──►  Ollama (LLM)
                              │                    │
                              ▼                    ▼
                        SharePoint         PostgreSQL + pgvector
                      (HR Documents)       (Vectors + History)
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3 |
| Database | PostgreSQL 16 + pgvector |
| AI / LLM | Ollama — `qwen2.5-coder` |
| Embeddings | Ollama — `nomic-embed-text` |
| Documents | SharePoint via Microsoft Graph |
| Interface | Microsoft Teams (Personal App) |
| Deployment | Docker Compose |

## Quick Start

```bash
cp .env.example .env   # Edit with your Azure/SharePoint credentials
docker compose up -d
curl -X POST http://localhost:8080/api/sync  # First document sync
# Then install the Teams app — see docs/deployment/teams-install.md
```

## Documentation

| Guide | Path |
|---|---|
| Deployment | docs/deployment/README.md |
| Ngrok | docs/deployment/ngrok-guide.md |
| SharePoint | docs/deployment/sharepoint-setup.md |
| Ollama | docs/deployment/ollama-setup.md |
| Teams Install | docs/deployment/teams-install.md |

## Features

- AI-powered HR answers with source citations
- Multilingual: Albanian, Serbian, English
- SharePoint auto-sync (hourly, PDF/DOCX/XLSX/PPTX)
- Semantic vector search via pgvector
- Conversation context (follow-up questions)
- Country-aware: Albania and Serbia policies separated
- Fully local — no paid AI services required
