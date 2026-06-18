package com.bytehr.service;

import java.util.List;

public interface ChunkingService {

    /**
     * Splits the given text into overlapping chunks.
     *
     * @param text         full document text
     * @param chunkSize    maximum characters per chunk
     * @param chunkOverlap characters of overlap between consecutive chunks
     * @return ordered list of text chunks
     */
    List<String> chunk(String text, int chunkSize, int chunkOverlap);
}
