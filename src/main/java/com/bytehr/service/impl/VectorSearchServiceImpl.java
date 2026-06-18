package com.bytehr.service.impl;

import com.bytehr.api.dto.RelevantChunk;
import com.bytehr.service.EmbeddingService;
import com.bytehr.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {

    /**
     * Score boost applied to EPR-named documents when the query contains performance/goal keywords.
     * Overcomes the language-bias in nomic-embed-text where same-language documents
     * (e.g., Serbian rulebooks) outscore semantically relevant English EPR documents.
     * Measured gap: rulebooks ~0.64, EPR docs ~0.41 → boost of 0.30 reverses ranking.
     */
    private static final double EPR_BOOST = 0.30;

    /** Keywords in the user's question that signal an EPR / performance-management query. */
    private static final Set<String> EPR_QUERY_KEYWORDS = Set.of(
            // English
            "goal", "goals", "objective", "objectives", "performance", "epr",
            "mid-year", "midyear", "review", "appraisal", "evaluation",
            // Serbian — cilj=goal, ocena=evaluation, pregled=review
            "cilj", "ciljeve", "ciljevi", "performanse", "pregled", "procena", "ocena",
            // Albanian — qëllim=goal, vlerësim=evaluation, objektiv=objective
            "qëllim", "qëllime", "performancë", "vlerësim", "objektiv"
    );

    /** Substrings matched against document filenames to identify EPR-related documents. */
    private static final Set<String> EPR_DOC_NAME_KEYWORDS = Set.of(
            "epr", "performance", "goal", "mid-year", "midyear", "review"
    );

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${vector-search.min-score}")
    private double minScore;

    @Override
    public List<RelevantChunk> search(String question, String country, int topK) {
        log.debug("[VectorSearch] Searching: question='{}', country='{}', topK={}", question, country, topK);

        float[] queryEmbedding = embeddingService.generateEmbedding(question);
        log.info("[VectorSearch] questionEmbeddingDimension={}", queryEmbedding.length);
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        // Log top-10 global candidates before any filtering — used for retrieval auditing
        logTopCandidates(vectorLiteral, 10);

        // Step 1: Standard country-scoped search
        List<RelevantChunk> results = new ArrayList<>();
        if (country != null && !country.isBlank()) {
            results = executeSearch(vectorLiteral, country, topK * 2);
            log.debug("[VectorSearch] Country search ({}): {} candidates", country, results.size());
        }
        if (results.isEmpty()) {
            results = executeSearch(vectorLiteral, null, topK * 2);
            log.debug("[VectorSearch] Global fallback: {} candidates", results.size());
        }

        // Step 2: EPR keyword boost
        // When the question is about goals/performance/EPR, also search for EPR-named
        // documents across ALL countries and apply a score boost to overcome language bias.
        if (isEprRelatedQuery(question)) {
            log.info("[VectorSearch] EPR keywords detected — executing EPR document boost");
            List<RelevantChunk> eprResults = executeEprSearch(vectorLiteral, topK * 2);
            log.info("[VectorSearch] EPR-specific candidates: {} (before boost)", eprResults.size());
            results = mergeWithEprBoost(results, eprResults, topK * 3);
            log.info("[VectorSearch] After EPR merge+boost: {} total candidates", results.size());
        }

        // Step 3: Apply min-score filter and limit to topK
        List<RelevantChunk> filtered = results.stream()
                .filter(c -> c.getSimilarityScore() >= minScore)
                .limit(topK)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.info("[VectorSearch] No chunks passed minScore={} filter (candidates={}, filtered=0). " +
                     "Consider lowering min-score or adding native-language documents.",
                     minScore, results.size());
        } else {
            log.info("[VectorSearch] retrievedDocuments={}: {}",
                    filtered.size(),
                    filtered.stream()
                            .map(c -> String.format("'%s'=%.3f", c.getDocumentName(), c.getSimilarityScore()))
                            .toList());
        }

        return filtered;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top-10 diagnostic logging
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Logs the top-N globally retrieved candidates BEFORE any min-score or country filtering.
     * Includes document name, raw score, country, and language for full diagnostic visibility.
     */
    private void logTopCandidates(String vectorLiteral, int n) {
        try {
            String sql = """
                    SELECT d.name, d.country, d.language,
                           1 - (dc.embedding <=> ?::vector) AS score
                    FROM document_chunks dc
                    JOIN documents d ON d.id = dc.document_id
                    WHERE dc.embedding IS NOT NULL
                    ORDER BY dc.embedding <=> ?::vector
                    LIMIT ?
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorLiteral, vectorLiteral, n);
            log.info("[VectorSearch] Top {} global candidates (before any filter):", n);
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                double score = ((Number) row.get("score")).doubleValue();
                log.info("[VectorSearch]   #{}: document='{}' score={} country={} language={}",
                        i + 1,
                        row.get("name"),
                        String.format("%.4f", score),
                        row.get("country"),
                        row.get("language"));
            }
        } catch (Exception e) {
            log.warn("[VectorSearch] Could not log top candidates: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EPR keyword boost
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isEprRelatedQuery(String question) {
        String lower = question.toLowerCase();
        return EPR_QUERY_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean isEprDocument(RelevantChunk chunk) {
        String nameLower = chunk.getDocumentName().toLowerCase();
        return EPR_DOC_NAME_KEYWORDS.stream().anyMatch(nameLower::contains);
    }

    /**
     * Searches for EPR-related documents across ALL countries using filename-based filtering.
     * Bypasses the country restriction so that EPR documents in albania-docs are found
     * even when the user's country is RS.
     */
    private List<RelevantChunk> executeEprSearch(String vectorLiteral, int topK) {
        String sql = """
                SELECT dc.id, dc.document_id, d.name, d.source_path,
                       dc.content, dc.chunk_index, dc.page_number,
                       1 - (dc.embedding <=> ?::vector) AS score
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE dc.embedding IS NOT NULL
                  AND (
                    LOWER(d.name) LIKE '%epr%' OR
                    LOWER(d.name) LIKE '%performance%' OR
                    LOWER(d.name) LIKE '%goal%' OR
                    LOWER(d.name) LIKE '%mid-year%' OR
                    LOWER(d.name) LIKE '%midyear%' OR
                    LOWER(d.name) LIKE '%review%'
                  )
                ORDER BY dc.embedding <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> RelevantChunk.builder()
                .chunkId(UUID.fromString(rs.getString("id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .documentName(rs.getString("name"))
                .sourcePath(rs.getString("source_path"))
                .content(rs.getString("content"))
                .chunkIndex(rs.getInt("chunk_index"))
                .pageNumber(rs.getObject("page_number") != null ? rs.getInt("page_number") : null)
                .similarityScore(rs.getDouble("score"))
                .build(), vectorLiteral, vectorLiteral, topK);
    }

    /**
     * Merges regular search results with EPR-specific results, applies a score boost to
     * EPR-named documents, deduplicates by chunk ID, and re-sorts by boosted score.
     */
    private List<RelevantChunk> mergeWithEprBoost(
            List<RelevantChunk> regular,
            List<RelevantChunk> eprSpecific,
            int maxResults) {

        Map<UUID, RelevantChunk> merged = new LinkedHashMap<>();
        regular.forEach(c -> merged.put(c.getChunkId(), c));
        eprSpecific.forEach(c -> merged.putIfAbsent(c.getChunkId(), c));

        return merged.values().stream()
                .map(c -> {
                    if (isEprDocument(c)) {
                        double boosted = Math.min(1.0, c.getSimilarityScore() + EPR_BOOST);
                        log.debug("[VectorSearch] EPR boost: '{}' {:.4f} → {:.4f}",
                                c.getDocumentName(), c.getSimilarityScore(), boosted);
                        return RelevantChunk.builder()
                                .chunkId(c.getChunkId())
                                .documentId(c.getDocumentId())
                                .documentName(c.getDocumentName())
                                .sourcePath(c.getSourcePath())
                                .content(c.getContent())
                                .chunkIndex(c.getChunkIndex())
                                .pageNumber(c.getPageNumber())
                                .similarityScore(boosted)
                                .build();
                    }
                    return c;
                })
                .sorted(Comparator.comparingDouble(RelevantChunk::getSimilarityScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard vector search
    // ─────────────────────────────────────────────────────────────────────────

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
