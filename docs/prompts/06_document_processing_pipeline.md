Implement the document ingestion pipeline.

Pipeline:

1. Extract text
2. Detect language
3. Chunk content
4. Generate embeddings
5. Store chunks

Create:

DocumentProcessor
LanguageDetectionService
ChunkingService
EmbeddingPipelineService

Requirements:

Chunk size:
1000 characters

Chunk overlap:
200 characters

Store vectors in pgvector.

Generate complete implementation.
