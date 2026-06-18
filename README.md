# ByteHR AI

An AI-powered HR Assistant integrated into Microsoft Teams, answering HR questions for employees in **Albania** and **Serbia** using official company documentation from SharePoint.

---

## Architecture

```
Microsoft Teams Personal App (Bot + Tab)
         │
         ▼
  ByteHR Spring Boot API  (port 8080)
         │
   ┌─────┴──────┐
   ▼            ▼
 Ollama      SharePoint (Microsoft Graph API)
 qwen3:14b        │
 nomic-embed      │
   │         ┌───┘
   └────┬────┘
        ▼
  PostgreSQL 16 + pgvector
```

## Tech Stack

| Layer       | Technology                              |
|-------------|----------------------------------------|
| Backend     | Java 21, Spring Boot 3.5, Maven        |
| AI          | Ollama, qwen3:14b, nomic-embed-text    |
| Database    | PostgreSQL 16 + pgvector               |
| Frontend    | Angular 21, Standalone Components, Signals, Angular Material |
| Integration | Microsoft Teams Bot, Microsoft Graph API |
| Deployment  | Docker Compose                          |

---

## Project Structure

```
byte-me/
├── src/                         # Spring Boot backend
│   ├── main/java/com/bytehr/
│   │   ├── api/                 # REST controllers + DTOs
│   │   ├── config/              # Spring configuration
│   │   ├── integration/         # Ollama + SharePoint clients
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Spring Data repositories
│   │   └── service/             # Business logic
│   └── main/resources/
│       ├── db/migration/        # Flyway SQL migrations (V1–V5)
│       └── application.yml
├── frontend/                    # Angular 21 application
│   ├── src/app/
│   │   ├── components/          # ChatWindow, MessageList, MessageInput, CitationPanel
│   │   ├── pages/               # ChatPage, SettingsPage
│   │   ├── services/            # ChatService, ConversationService, CitationService
│   │   └── models/              # TypeScript interfaces
│   ├── Dockerfile
│   └── nginx.conf
├── teams/                       # Microsoft Teams app package
│   ├── manifest.json
│   ├── color.png
│   └── outline.png
├── docs/                        # Documentation and guides
│   ├── business-specification.md
│   └── deployment/
├── docker-compose.yml
├── Dockerfile                   # Backend Dockerfile
└── pom.xml
```

---

## Quick Start

### Prerequisites

- Docker Desktop (with Compose v2)
- A Microsoft Azure AD application (for Teams bot + SharePoint)
- An ngrok account (for Teams tunneling)

### 1. Configure Environment

```bash
cp .env.example .env
# Edit .env with your values:
# SHAREPOINT_TENANT_ID, SHAREPOINT_CLIENT_ID, SHAREPOINT_CLIENT_SECRET
# SHAREPOINT_SITE_ID, SHAREPOINT_DRIVE_ID
# TEAMS_APP_ID, TEAMS_APP_PASSWORD
```

### 2. Start All Services

```bash
docker compose up -d
```

This starts:
- PostgreSQL 16 with pgvector
- Ollama (pulls `nomic-embed-text` and `qwen3:14b` automatically)
- ByteHR Spring Boot API on port 8080
- ByteHR Angular Frontend on port 4200

### 3. Verify Health

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## API Reference

### POST /api/chat
Submit an HR question and receive an AI-generated answer.

**Request:**
```json
{
  "message": "How many vacation days do I have?",
  "conversationId": "optional-uuid",
  "userId": "user-aad-id",
  "country": "AL"
}
```

**Response:**
```json
{
  "answer": "According to the Annual Leave Policy, full-time employees are entitled to 20 working days...",
  "citations": [
    {
      "documentName": "Annual-Leave-Policy-Albania.pdf",
      "pageNumber": 3,
      "sourcePath": "https://sharepoint.example.com/..."
    }
  ],
  "confidenceScore": 0.92,
  "detectedLanguage": "en",
  "answered": true
}
```

### POST /api/sync
Manually trigger SharePoint document synchronization.

### GET /api/conversations/{conversationId}
Retrieve conversation history.

### GET /api/analytics
Get usage and quality analytics summary.

---

## Microsoft Teams Setup

1. Register a Bot in Azure Bot Service
2. Set the Bot messaging endpoint to: `https://<your-ngrok-domain>/api/messages`
3. Update `teams/manifest.json` — replace `${{TEAMS_APP_ID}}` and `${{NGROK_DOMAIN}}`
4. Package: `cd teams && zip -r ByteHR.zip manifest.json color.png outline.png`
5. Upload to Teams: Teams Admin Center → Manage Apps → Upload

See `docs/deployment/teams-install.md` for full instructions.

---

## SharePoint Setup

1. Register an Azure AD application with permissions:
   - `Sites.Read.All`
   - `Files.Read.All`
2. Create a SharePoint document library with HR documents (PDF, DOCX, XLSX, PPTX)
3. Configure environment variables with tenant ID, client ID, secret, site ID, drive ID

See `docs/deployment/sharepoint-setup.md` for full instructions.

---

## Ollama Setup (local development)

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull required models
ollama pull nomic-embed-text
ollama pull qwen3:14b
```

See `docs/deployment/ollama-setup.md` for details.

---

## Language Support

| Language | Detection | Response |
|----------|-----------|---------|
| English  | ✅        | ✅      |
| Albanian | ✅        | ✅      |
| Serbian  | ✅        | ✅      |

ByteHR AI automatically detects the question language and responds in the same language.

---

## Country-Specific Policy Filtering

- **Albania (AL)** — Albanian HR policies take priority
- **Serbia (RS)** — Serbian HR policies take priority
- Users only see policies relevant to their country
- Country is resolved from the Teams user's Azure AD profile (`usageLocation`)

---

## Running Tests

```bash
mvn test
```

Tests included:
- `ChunkingServiceTest` — document chunking logic
- `LanguageDetectionServiceTest` — language detection
- `HrResponseAgentTest` — RAG response generation (mocked)
- `ChatControllerTest` — REST API layer
- `SyncControllerTest` — sync endpoint
- `DocumentIngestionPipelineTest` — integration pipeline

---

## Development (without Docker)

```bash
# Start PostgreSQL
docker compose up postgres -d

# Start Ollama
ollama serve

# Run backend
mvn spring-boot:run

# Run frontend
cd frontend && npm install && npm start
```

---

## License

Internal use only — ByteHR Team.
