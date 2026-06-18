Implement Ollama integration.

Models:

Embedding:

* nomic-embed-text

Chat:

* qwen2.5-coder

Requirements:

Create:

* OllamaClient
* EmbeddingService
* ChatService

Functions:

generateEmbedding(text)

generateAnswer(question, context)

Use REST communication.

Configuration must be externalized in application.yml.

Generate production-ready code.
