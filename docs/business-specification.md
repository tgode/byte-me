# ByteHR AI - Business Specification

**Version:** 1.0  
**Status:** Ready for Implementation  
**Target:** Hackathon MVP

---

# 1. Overview

ByteHR AI is an AI-powered Human Resources Assistant integrated directly into Microsoft Teams.

The solution enables employees from Albania and Serbia to ask HR-related questions in natural language and receive immediate answers based on official company documentation stored in SharePoint.

The platform eliminates repetitive HR requests, improves information accessibility, and provides multilingual support without requiring a new enterprise application.

Microsoft Teams will be the only user interface.

---

# 2. Business Problem

HR departments repeatedly answer the same questions regarding:

- Vacation policies
- Sick leave
- Public holidays
- Employee benefits
- Payroll schedules
- Travel reimbursement
- Remote work policies
- Equipment requests
- Onboarding procedures
- Internal regulations

Although this information already exists in company documentation, employees often:

- Do not know where documents are located
- Cannot find the correct information quickly
- Access outdated documents
- Require answers in their native language

This results in unnecessary workload for HR teams and slower employee support.

---

# 3. Business Objectives

## Primary Objective

Reduce repetitive HR requests by providing a self-service AI assistant.

## Secondary Objectives

- Provide 24/7 HR support.
- Improve employee access to company policies.
- Ensure answers are based on official company documentation.
- Support multilingual communication.
- Minimize HR operational workload.

---

# 4. Scope

## In Scope

- Microsoft Teams integration
- SharePoint integration
- HR document ingestion
- AI-powered question answering
- Multilingual support
- Source citations
- Conversation context
- Vector-based semantic search
- Country-specific policy handling
- Local AI inference using Ollama

## Out of Scope

- Payroll management
- Leave approval workflows
- Employee record management
- Performance management
- SAP integrations
- Active Directory synchronization
- Ticketing workflows
- Mobile applications
- Custom enterprise portals

---

# 5. Supported Regions

## Albania

Supported Language:

- Albanian

## Serbia

Supported Language:

- Serbian

## Global

Supported Language:

- English

The system shall understand and respond in all supported languages.

---

# 6. User Roles

## Employee

Responsibilities:

- Ask HR-related questions
- Search company policies
- View document references
- Continue conversations
- Receive responses in preferred language

## (Optional) HR Administrator

Responsibilities:

- Configure SharePoint sources
- Trigger document synchronization
- Monitor chatbot activity
- Review analytics
- Manage chatbot settings

This role is optional, since at the moment the main role is the Employee who can access the chatbot in MS Teams.

---

# 7. Functional Requirements

## FR-01 Microsoft Teams Integration

The solution shall be implemented as a Microsoft Teams application.

No standalone web application shall be required.

---

## FR-02 SharePoint Integration

SharePoint shall be the single source of truth for HR documentation.

Supported document formats:

- PDF
- DOCX
- XLSX
- PPTX

---

## FR-03 Document Synchronization

The system shall automatically synchronize documents from SharePoint.

Default synchronization interval:

- Every 1 hour

New and modified documents shall be automatically processed.

---

## FR-04 Multilingual Support

The system shall:

- Understand Albanian
- Understand Serbian
- Understand English

The response language shall match the language used by the employee.

---

## FR-05 Document Processing

The system shall:

- Extract document content
- Detect document language
- Split content into searchable chunks
- Generate vector embeddings
- Store vector embeddings for semantic search

---

## FR-06 Semantic Search

The system shall retrieve relevant information based on meaning rather than exact keyword matching.

Example:

Question:

> Can I work remotely?

Document Content:

> Employees are allowed to work from home.

The system shall correctly retrieve the relevant policy.

---

## FR-07 Answer Generation

The system shall generate answers exclusively from company documentation.

The assistant shall not generate unsupported company policies, or other questions not related to the scope of the application.

Example:

Question:

> What is the weather in Serbia?

Chatbot answer:

> The question cannot be answered. Can I help you with <suggestion>?

The system shall correctly answer only for the context of the document and HR related domain, with the relevant policy.

---

## FR-08 Source Citations

Every answer shall include:

- Document name
- Relevant section
- Source reference

Example:

```text
Source:
Employee Handbook 2026
Vacation Policy
Page 14
```

Inside the answer, the citations can be clickable and preview the part of the document where it is retrieved from.

---

## FR-09 Conversation Context

The assistant shall maintain conversational context, up to a certain limit of tokens.

Follow-up questions shall be understood without repeating previous information.

---

## FR-10 Confidence Management

When confidence is below the configured threshold, the assistant shall respond:

```text
I could not find a reliable answer. Please contact HR.
```

---

## FR-11 Country Awareness

The assistant shall prioritize policies according to employee country.

Supported countries:

- Albania
- Serbia

Country-specific policies shall override generic policies.
Chatbot cannot answer questions done for documents that are part of the policies of the other region/country than the one currently. The information can be retrieved from user's MS Teams Profile.

---

# 8. AI Components

## Document Ingestion Agent

Responsibilities:

- Read SharePoint documents
- Extract content
- Detect language
- Generate embeddings
- Store vectors

## Retrieval Agent

Responsibilities:

- Receive user questions
- Generate query embeddings
- Search vector database
- Retrieve relevant document chunks

## HR Response Agent

Responsibilities:

- Generate final answer
- Include source citations
- Apply country-specific logic
- Return confidence score

---

# 9. System Architecture

```text
Microsoft Teams
        |
        v
ByteHR Teams App
        |
        v
Spring Boot API
        |
        +------------------------+
        |                        |
        v                        v
      Ollama                SharePoint
        |                        |
        |                        |
        +------------+-----------+
                     |
                     v
            PostgreSQL + pgvector
```

---

# 10. Technology Stack

## Backend

- Java 21
- Spring Boot 3

## Database

- PostgreSQL 16
- pgvector

## AI

- Ollama
- qwen2.5-coder
- nomic-embed-text

## Integrations

- Microsoft Teams
- Microsoft Graph API
- SharePoint

## Deployment

- Docker Compose

---

# 11. Data Model

## Documents

Stores:

- Document metadata
- Source location
- Language
- Synchronization timestamp, always update the newer version of the documents.

## Document Chunks

Stores:

- Chunk content
- Document reference
- Embedding vector

## Conversations

Stores:

- User question
- Generated response
- Timestamp

## Analytics

Stores:

- Total questions
- Response times
- Confidence scores
- Frequently asked questions

---

# 12. Processing Pipeline

```text
SharePoint Documents
        |
        v
Document Synchronization
        |
        v
Text Extraction
        |
        v
Language Detection
        |
        v
Document Chunking
        |
        v
Embedding Generation
        |
        v
PostgreSQL + pgvector
        |
        v
Employee Question
        |
        v
Question Embedding
        |
        v
Vector Search
        |
        v
Relevant Chunks
        |
        v
Ollama
        |
        v
Final Response + Citations
```

---

# 13. Non-Functional Requirements

## NFR-01

Average response time shall be less than 10 seconds.

## NFR-02

Document synchronization shall not impact user requests.

## NFR-03

Only indexed company documents shall be used as answer sources.

## NFR-04

Every response shall contain source citations.

## NFR-05

The solution shall support at least 1,000 indexed documents.

## NFR-06

The solution shall support concurrent employee usage.

## NFR-07

The system shall automatically process newly uploaded HR documents.

---

# 14. Demo Scenario

1. Employee opens Microsoft Teams.
2. Employee opens ByteHR AI.
3. Employee asks: "How many vacation days do I have?"
4. The assistant searches indexed HR documents.
5. The assistant generates an answer.
6. The assistant displays source citations.
7. The employee asks a follow-up question.
8. The assistant maintains conversation context.
9. The employee asks the same question in Albanian.
10. The assistant responds in Albanian.
11. The employee asks the same question in Serbian.
12. The assistant responds in Serbian.

---

# 15. Implementation Constraints

The implementation must:

- Use Microsoft Teams as the only user interface.
- Use SharePoint as the source of truth.
- Use PostgreSQL with pgvector for vector storage.
- Use Ollama for local AI inference.
- Support Albanian, Serbian and English.
- Be deployable locally using Docker Compose.
- Require no paid AI services.
- Be fully functional in a local development environment.

---

# 16. Success Criteria

The MVP is considered successful when:

- Employees can ask HR questions through Microsoft Teams.
- Documents are automatically synchronized from SharePoint.
- Questions are answered using company documentation.
- Responses include citations.
- Multilingual support works correctly.
- Context-aware conversations are supported.
- Average response time remains below five seconds.
- No additional enterprise application is required.

---

# 17. Microsoft Teams Application Requirements

## Teams Application Type

The solution shall be implemented as a Microsoft Teams Personal Application.

The Teams application shall serve as the primary and only user interface for employees.

No standalone web application shall be required for end users.

---

## Teams Deliverables

The implementation shall generate all artifacts required for Teams deployment, including:

- Teams Application Manifest
- Application Icons
- Teams Application Package (.zip)
- Teams Personal App Configuration
- Teams Tab Configuration
- Backend Integration Configuration

---

## Teams User Experience

Employees shall be able to:

- Install ByteHR AI from Teams
- Pin ByteHR AI to the Teams sidebar
- Open the assistant directly from Teams
- Chat with the assistant using natural language
- View source citations
- Continue conversations

---

## Teams Installation

The generated solution shall support deployment through Teams Custom App Upload.

The application shall not require publication to the Microsoft Teams Store.

Deployment steps shall be documented and generated as part of the solution.

---

## Teams Manifest Generation

The implementation shall generate:

manifest.json

Required application icons:

- color.png
- outline.png

The generated files shall be packageable as:

ByteHR.zip

- manifest.json
- color.png
- outline.png

---

## Teams Authentication

The application shall support Microsoft Teams authentication and user identification.

User information available to the assistant:

- Display Name
- Email
- Country
- Department (if available)

---

## Teams Deployment Documentation

The solution shall include:

- Local development setup guide
- Docker deployment guide
- Teams application installation guide
- Ngrok configuration guide for local testing
- SharePoint connection setup guide
- Ollama setup guide

---

## Teams MVP Requirement

A user shall be able to:

1. Open Microsoft Teams.
2. Open ByteHR AI from the Teams sidebar.
3. Ask an HR question.
4. Receive a response generated from SharePoint documents.
5. View source citations.
6. Continue the conversation.

This workflow is mandatory for MVP acceptance.

End of Specification 
