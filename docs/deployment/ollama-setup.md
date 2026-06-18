# Ollama Setup Guide

Ollama provides local AI inference without any cloud dependencies.

---

## Automatic Setup (Docker Compose)

When you run `docker compose up`, the `ollama-init` service automatically pulls:
- `nomic-embed-text` — 768-dimensional text embeddings
- `qwen3:1.7b` — HR answer generation (multilingual)

No manual action required.

---

## Manual Setup (Local Development without Docker)

### 1. Install Ollama

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows: download from https://ollama.com/download
```

### 2. Pull Models

```bash
ollama pull nomic-embed-text
ollama pull qwen3:1.7b
```

### 3. Verify

```bash
# List installed models
ollama list

# Test embedding
curl http://localhost:11434/api/embeddings \
  -d '{"model":"nomic-embed-text","prompt":"Hello HR"}'

# Test chat
curl http://localhost:11434/api/chat \
  -d '{"model":"qwen3:1.7b","messages":[{"role":"user","content":"What is vacation policy?"}],"stream":false}'
```

---

## Resource Requirements

| Model | Disk | RAM (approx) |
|---|---|---|
| nomic-embed-text | ~270 MB | ~1 GB |
| qwen3:1.7b | ~5.2 GB | ~6 GB |

Minimum recommended: **8 GB RAM**, **10 GB disk**.

---

## Configuration

Set the Ollama base URL in `.env`:

```env
OLLAMA_BASE_URL=http://localhost:11434   # local dev
# or
OLLAMA_BASE_URL=http://ollama:11434      # Docker Compose (uses service name)
```
