Implement semantic retrieval.

Create:

VectorSearchService

Requirements:

Input:

* user question
* country

Process:

1. Generate question embedding
2. Search pgvector
3. Filter by country policy
4. Return top relevant chunks

Return:

* chunk content
* document reference
* similarity score

Generate production-ready implementation.
