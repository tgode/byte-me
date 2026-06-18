package com.bytehr.service.impl;

import com.bytehr.api.dto.RelevantChunk;
import com.bytehr.service.EmbeddingService;
import com.bytehr.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${vector-search.min-score}")
    private double minScore;

    @Override
    public List<RelevantChunk> search(String question, String country, int topK) {
        log.debug("[VectorSearch] Searching: question='{}', country='{}', topK={}", question, country, topK);

        float[] queryEmbedding = embeddingService.generateEmbedding(question);
        log.debug("[VectorSearch] Query embedding generated: dimension={}", queryEmbedding.length);
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        List<RelevantChunk> results = new ArrayList<>();

        if (country != null && !country.isBlank()) {
            results = executeSearch(vectorLiteral, country, topK);
            log.debug("[VectorSearch] Country-specific search (country={}): {} chunks returned", country, results.size());
        }

        if (results.isEmpty()) {
            results = executeSearch(vectorLiteral, null, topK);
            log.debug("[VectorSearch] Global fallback search: {} chunks returned", results.size());
        }

        List<RelevantChunk> filtered = results.stream()
                .filter(c -> c.getSimilarityScore() >= minScore)
                .toList();

        log.debug("[VectorSearch] After minScore={} filter: {} chunks remain (scores: {})",
                minScore, filtered.size(),
                filtered.stream().map(c -> String.format("%.3f", c.getSimilarityScore())).toList());

        return filtered;
    }

    private List<RelevantChunk> executeSearch(String vectorLiteral, String country, int topK) {
        String sql;
        Object[] params;

        if (country != null) {
            sql = """
                    SELECT dc.id, dc.document_id, d.name, d.source_path,
                           dc.content, dc.chunk_index, dc.page_number,
                           1 - (dc.embedding <=> ?::vector) AS score
                    FROM document_chunks dc
                    JOIN documents d ON d.id = dc.document_id
                    WHERE dc.embedding IS NOT NULL
                      AND (d.country = ? OR d.country IS NULL)
                    ORDER BY dc.embedding <=> ?::vector
                    LIMIT ?
                    """;
            params = new Object[]{vectorLiteral, country.toUpperCase(), vectorLiteral, topK};
        } else {
            sql = """
                    SELECT dc.id, dc.document_id, d.name, d.source_path,
                           dc.content, dc.chunk_index, dc.page_number,
                           1 - (dc.embedding <=> ?::vector) AS score
                    FROM document_chunks dc
                    JOIN documents d ON d.id = dc.document_id
                    WHERE dc.embedding IS NOT NULL
                    ORDER BY dc.embedding <=> ?::vector
                    LIMIT ?
                    """;
            params = new Object[]{vectorLiteral, vectorLiteral, topK};
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> RelevantChunk.builder()
                .chunkId(UUID.fromString(rs.getString("id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .documentName(rs.getString("name"))
                .sourcePath(rs.getString("source_path"))
                .content(rs.getString("content"))
                .chunkIndex(rs.getInt("chunk_index"))
                .pageNumber(rs.getObject("page_number") != null ? rs.getInt("page_number") : null)
                .similarityScore(rs.getDouble("score"))
                .build());
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
