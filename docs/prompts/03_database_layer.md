Implement the database layer.

Create entities for:

Documents
DocumentChunks
Conversations
Analytics

Requirements:

Documents:

* id
* name
* sourcePath
* language
* lastSync

DocumentChunks:

* id
* documentId
* content
* embeddingVector

Conversations:

* id
* userId
* question
* response
* timestamp

Analytics:

* id
* responseTime
* confidenceScore
* timestamp

Generate:

* JPA entities
* repositories
* Liquibase migrations

Do not implement service logic.
