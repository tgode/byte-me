# ByteHR AI — Deployment Guide

## Prerequisites

- Docker 24+ and Docker Compose v2
- A Microsoft Azure account (for Teams Bot and SharePoint)
- Git

---

## 1. Clone & Configure

```bash
git clone https://github.com/your-org/byte-hr.git
cd byte-hr
cp .env.example .env
# Edit .env with your actual credentials
```

---

## 2. Azure Setup

### 2a. Register an Azure Bot

1. Go to [Azure Portal](https://portal.azure.com) → **Azure Bot** → Create
2. Choose **Multi Tenant** and create a new **App Registration**
3. Note the **App ID** and generate a **Client Secret**
4. Under **Channels**, add **Microsoft Teams**
5. Set the **Messaging Endpoint** to:
   ```
   https://<your-ngrok-or-domain>/api/messages
   ```
6. Copy the App ID and secret into `.env`:
   ```
   TEAMS_APP_ID=<app-id>
   TEAMS_APP_PASSWORD=<client-secret>
   ```

### 2b. SharePoint App Registration

See [docs/deployment/sharepoint-setup.md](sharepoint-setup.md).

---

## 3. Start the Stack

```bash
docker compose up -d
```

This starts:
| Service | Port | Description |
|---|---|---|
| `bytehr-api` | 8080 | Spring Boot API |
| `postgres` | 5432 | PostgreSQL 16 + pgvector |
| `ollama` | 11434 | Local LLM inference |
| `ollama-init` | — | Pulls AI models once |

> First startup may take **10–20 minutes** while Ollama downloads the models (~4 GB total).

### Verify the stack

```bash
# API health
curl http://localhost:8080/actuator/health

# Ollama models
curl http://localhost:11434/api/tags
```

---

## 4. Expose API with Ngrok (Local Testing)

See [docs/deployment/ngrok-guide.md](ngrok-guide.md).

---

## 5. Install Teams App

See [docs/deployment/teams-install.md](teams-install.md).

---

## 6. First Document Sync

Once SharePoint is configured, trigger an initial sync:

```bash
curl -X POST http://localhost:8080/api/sync
```

Or wait for the automatic hourly sync to run.

---

## Useful Commands

```bash
# View API logs
docker compose logs -f bytehr-api

# Rebuild API after code changes
docker compose up -d --build bytehr-api

# Stop everything
docker compose down

# Stop and remove volumes (full reset)
docker compose down -v
```
