# ByteHR AI - Global Implementation Rules

You are implementing ByteHR AI.

Before generating any code, follow these mandatory rules:

## Business Scope

The system is an AI-powered HR Assistant integrated into Microsoft Teams.

Supported countries:

* Albania
* Serbia

Supported languages:

* Albanian
* Serbian
* English

The assistant answers ONLY HR-related questions using indexed company documents.

## Technology Constraints

Backend:

* Java 21
* Spring Boot 3

Database:

* PostgreSQL 16
* pgvector

AI:

* Ollama
* nomic-embed-text
* qwen2.5-coder

Integrations:

* Microsoft Teams
* Microsoft Graph API
* SharePoint

Deployment:

* Docker Compose

## Strict Rules

DO NOT:

* Invent requirements
* Add features not present in the specification
* Add payroll functionality
* Add leave approval workflows
* Add SAP integrations
* Add Active Directory synchronization
* Add ticketing systems
* Add mobile applications

ONLY implement features explicitly described in the business specification.

## Architecture Rules

Use clean architecture:

api/
service/
repository/
model/
integration/
config/

Generate production-quality code.

Prefer interfaces for services.

Use constructor injection only.

No field injection.

## AI Rules

The assistant must:

* Answer only from retrieved documents.
* Never hallucinate policies.
* Reject unrelated questions.
* Return citations with every answer.
* Return confidence scores internally.

If confidence is low:

"I could not find a reliable answer. Please contact HR."

## Output Rules

For every implementation step:

1. Explain architecture decisions.
2. Generate folder structure.
3. Generate code.
4. Explain configuration.
5. Provide run instructions.

Never skip steps.

