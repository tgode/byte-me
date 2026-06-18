package com.bytehr.service.impl;

import com.bytehr.service.ChunkingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    @Override
    public List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Normalize whitespace
        String normalized = text.replaceAll("\\s+", " ").trim();
        int length = normalized.length();
        int step = chunkSize - chunkOverlap;

        if (step <= 0) {
            throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
        }

        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= length) break;
            start += step;
        }
        return chunks;
    }
}
