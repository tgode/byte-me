package com.bytehr.service.impl;

import com.bytehr.integration.sharepoint.SharePointClient;
import com.bytehr.integration.sharepoint.dto.SharePointFile;
import com.bytehr.model.Document;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.DocumentProcessorService;
import com.bytehr.service.DocumentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentSyncServiceImpl implements DocumentSyncService {

    private final SharePointClient sharePointClient;
    private final DocumentRepository documentRepository;
    private final DocumentProcessorService documentProcessorService;

    @Override
    @Scheduled(fixedRateString = "${sharepoint.sync-interval-ms:3600000}",
               initialDelay = 10000)
    public void synchronize() {
        log.info("Starting SharePoint document synchronization...");
        AtomicInteger processed = new AtomicInteger(0);

        List<SharePointFile> remoteFiles = sharePointClient.listDocuments();
        if (remoteFiles.isEmpty()) {
            log.info("No documents found in SharePoint or sync disabled.");
            return;
        }

        for (SharePointFile remoteFile : remoteFiles) {
            try {
                Optional<Document> existing = documentRepository.findBySharepointItemId(remoteFile.getId());

                boolean isNew = existing.isEmpty();
                boolean isModified = existing.isPresent()
                        && remoteFile.getLastModified() != null
                        && existing.get().getLastModified() != null
                        && remoteFile.getLastModified().isAfter(existing.get().getLastModified());

                if (isNew || isModified) {
                    Document document = isNew
                            ? Document.builder()
                                .name(remoteFile.getName())
                                .sourcePath(remoteFile.getWebUrl())
                                .sharepointItemId(remoteFile.getId())
                                .fileType(remoteFile.getFileExtension())
                                .fileSize(remoteFile.getSize())
                                .lastModified(remoteFile.getLastModified())
                                .build()
                            : existing.get();

                    if (!isNew) {
                        document.setLastModified(remoteFile.getLastModified());
                        document.setFileSize(remoteFile.getSize());
                    }

                    document = documentRepository.save(document);
                    byte[] content = sharePointClient.downloadDocument(remoteFile.getId());
                    documentProcessorService.processDocument(document, content);
                    processed.incrementAndGet();
                    log.info("Synchronized document: {}", remoteFile.getName());
                }
            } catch (Exception e) {
                log.error("Failed to process SharePoint file: {}", remoteFile.getName(), e);
            }
        }

        log.info("SharePoint synchronization complete. Documents processed: {}", processed.get());
    }
}
