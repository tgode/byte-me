package com.bytehr.api;

import com.bytehr.api.dto.SyncResponse;
import com.bytehr.service.DocumentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint to manually trigger SharePoint document synchronization.
 */
@RestController
@RequestMapping("/api/sync")
@Slf4j
@RequiredArgsConstructor
public class SyncController {

    private final DocumentSyncService documentSyncService;

    @PostMapping
    public ResponseEntity<SyncResponse> triggerSync() {
        log.info("Manual sync triggered via API");
        try {
            documentSyncService.synchronize();
            return ResponseEntity.ok(SyncResponse.builder()
                    .status("success")
                    .message("Synchronization completed successfully.")
                    .build());
        } catch (Exception e) {
            log.error("Manual sync failed", e);
            return ResponseEntity.internalServerError()
                    .body(SyncResponse.builder()
                            .status("error")
                            .message("Synchronization failed: " + e.getMessage())
                            .build());
        }
    }
}
