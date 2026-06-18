package com.bytehr.service;

import com.bytehr.api.dto.RelevantChunk;

import java.util.List;

public interface VectorSearchService {

    /**
     * Searches for the most semantically relevant document chunks for the given question,
     * optionally filtering by the user's country.
     *
     * @param question user question text
     * @param country  ISO-2 country code (e.g. "AL", "RS") or null for no filtering
     * @param topK     maximum number of chunks to return
     * @return ranked list of relevant chunks with similarity scores
     */
    List<RelevantChunk> search(String question, String country, int topK);
}
