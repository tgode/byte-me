package com.bytehr.service;

import com.bytehr.config.SourceProperties;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.impl.LocalDocumentSourceServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupValidationService implements ApplicationRunner {

    private final SourceProperties sourceProperties;
    private final DocumentRepository documentRepository;
    private final DocumentSyncService documentSyncService;

    @Override
    public void run(ApplicationArguments args) {
        String type = sourceProperties.getType().toUpperCase();
        String path = sourceProperties.getLocalPath();
        long indexedDocs = 0;
        int discoverable = 0;

        try {
            indexedDocs = documentRepository.count();
        } catch (Exception e) {
            log.debug("Could not query document count at startup (DB may not be ready yet): {}", e.getMessage());
        }

        if ("LOCAL".equals(type) && documentSyncService instanceof LocalDocumentSourceServiceImpl local) {
            discoverable = local.countDiscoverableFiles();
            Path absPath = Path.of(System.getProperty("user.dir")).resolve(path);
            boolean pathExists = Files.exists(absPath);
            log.info("\n" +
                    "╔══════════════════════════════════════════════╗\n" +
                    "║              ByteHR AI  —  Startup           ║\n" +
                    "╠══════════════════════════════════════════════╣\n" +
                    "║  Source Type  : {:<30}║\n" +
                    "║  Source Path  : {:<30}║\n" +
                    "║  Path Exists  : {:<30}║\n" +
                    "║  Files Found  : {:<30}║\n" +
                    "║  Indexed Docs : {:<30}║\n" +
                    "╚══════════════════════════════════════════════╝",
                    type, path,
                    pathExists ? "YES" : "NO — run POST /api/sync after mount",
                    discoverable,
                    indexedDocs);
        } else {
            log.info("\n" +
                    "╔══════════════════════════════════════════════╗\n" +
                    "║              ByteHR AI  —  Startup           ║\n" +
                    "╠══════════════════════════════════════════════╣\n" +
                    "║  Source Type  : {:<30}║\n" +
                    "║  Indexed Docs : {:<30}║\n" +
                    "╚══════════════════════════════════════════════╝",
                    type, indexedDocs);
        }
    }
}
