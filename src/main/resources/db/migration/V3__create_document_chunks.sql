-- Text chunks extracted from HR documents with pgvector embeddings (nomic-embed-text → 768 dims)
CREATE TABLE IF NOT EXISTS document_chunks (
    id           UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id  UUID    NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content      TEXT    NOT NULL,
    chunk_index  INTEGER NOT NULL,
    page_number  INTEGER,
    embedding    vector(768),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks (document_id);

-- IVFFlat index for approximate nearest-neighbour cosine similarity search
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
