package com.bytehr.service;

import com.bytehr.model.Document;

public interface DocumentProcessorService {

    /**
     * Processes a document: extracts text, detects language, chunks, generates embeddings,
     * and stores everything in the database.
     *
     * @param document    the already-persisted Document entity
     * @param fileContent raw bytes of the document
     */
    void processDocument(Document document, byte[] fileContent);
}
