package com.bytehr.api;

import com.bytehr.api.dto.ReindexResponse;
import com.bytehr.repository.DocumentChunkRepository;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.DocumentSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin operations — full reindex and other maintenance tasks.
 * Not intended for end-user access; secure in production with authentication.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative operations — full reindex, maintenance")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentSyncService documentSyncService;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/reindex")
    @Operation(
        summary = "Full document reindex",
        description = """
            Clears all indexed documents and chunks, then re-processes every
            document from the configured source (local or SharePoint).
            
            Steps:
            1. Delete all document_chunks (vectors cleared)
            2. Delete all documents
            3. Re-run document synchronization (full ingestion pipeline)
            4. Return processing statistics
            
            Warning: this is a destructive operation. All vectors are dropped
            and regenerated. This may take several minutes depending on the
            number of documents and the Ollama embedding model speed.
            """
    )
    public ResponseEntity<ReindexResponse> reindex() {
        long start = System.currentTimeMillis();
        log.info("[Reindex] Full reindex triggered via POST /api/admin/reindex");

        try {
            // Step 1: Clear all chunks and documents
            long oldChunks = chunkRepository.count();
            long oldDocs   = documentRepository.count();
            chunkRepository.deleteAll();
            documentRepository.deleteAll();
            log.info("[Reindex] Cleared {} documents and {} chunks", oldDocs, oldChunks);

            // Step 2: Re-synchronize (treats everything as new)
            int processed = documentSyncService.synchronize();

            // Step 3: Collect stats
            long totalChunks = chunkRepository.count();
            Long totalEmbeddings = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE embedding IS NOT NULL",
                    Long.class);
            if (totalEmbeddings == null) totalEmbeddings = 0L;

            long durationMs = System.currentTimeMillis() - start;
            log.info("[Reindex] Complete: docs={}, chunks={}, embeddings={}, duration={}ms",
                    processed, totalChunks, totalEmbeddings, durationMs);

            return ResponseEntity.ok(ReindexResponse.builder()
                    .status("success")
                    .documentsProcessed(processed)
                    .chunksCreated(totalChunks)
                    .embeddingsCreated(totalEmbeddings)
                    .durationMs(durationMs)
                    .message(processed + " documents processed, " +
                             totalChunks + " chunks created, " +
                             totalEmbeddings + " embeddings generated.")
                    .build());

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("[Reindex] Failed after {}ms: {}", durationMs, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ReindexResponse.builder()
                            .status("error")
                            .durationMs(durationMs)
                            .message("Reindex failed: " + e.getMessage())
                            .build());
        }
    }
}
