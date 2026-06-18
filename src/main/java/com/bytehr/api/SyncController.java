package com.bytehr.api;

import com.bytehr.api.dto.SyncResponse;
import com.bytehr.config.SourceProperties;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.DocumentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint to trigger document synchronization (works for both local and SharePoint sources).
 */
@RestController
@RequestMapping("/api/sync")
@Slf4j
@RequiredArgsConstructor
public class SyncController {

    private final DocumentSyncService documentSyncService;
    private final DocumentRepository documentRepository;
    private final SourceProperties sourceProperties;

    @PostMapping
    public ResponseEntity<SyncResponse> triggerSync() {
        log.info("Manual sync triggered via API (source={})", sourceProperties.getType());
        long start = System.currentTimeMillis();
        try {
            int processed = documentSyncService.synchronize();
            long total = documentRepository.count();
            return ResponseEntity.ok(SyncResponse.builder()
                    .status("success")
                    .documentsProcessed(processed)
                    .totalDocumentsIndexed(total)
                    .durationMs(System.currentTimeMillis() - start)
                    .sourceType(sourceProperties.getType())
                    .message(processed + " document(s) processed. " + total + " total indexed.")
                    .build());
        } catch (Exception e) {
            log.error("Manual sync failed", e);
            return ResponseEntity.internalServerError()
                    .body(SyncResponse.builder()
                            .status("error")
                            .sourceType(sourceProperties.getType())
                            .durationMs(System.currentTimeMillis() - start)
                            .message("Synchronization failed: " + e.getMessage())
                            .build());
        }
    }
}
