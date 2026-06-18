package com.bytehr.service;

import com.bytehr.config.SourceProperties;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.impl.LocalDocumentSourceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class StartupValidationService implements ApplicationRunner {

    private final SourceProperties sourceProperties;
    private final DocumentRepository documentRepository;
    private final DocumentSyncService documentSyncService;
    private final String chatModel;
    private final String embeddingModel;

    public StartupValidationService(
            SourceProperties sourceProperties,
            DocumentRepository documentRepository,
            DocumentSyncService documentSyncService,
            @Value("${ollama.chat-model}") String chatModel,
            @Value("${ollama.embedding-model}") String embeddingModel) {
        this.sourceProperties = sourceProperties;
        this.documentRepository = documentRepository;
        this.documentSyncService = documentSyncService;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

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

        // Log model configuration on every startup
        log.info("[Model Config]");
        log.info("[Model Config] Chat Model     : {}", chatModel);
        log.info("[Model Config] Embedding Model: {}", embeddingModel);

        if ("LOCAL".equals(type) && documentSyncService instanceof LocalDocumentSourceServiceImpl local) {
            discoverable = local.countDiscoverableFiles();
            Path configuredPath = Path.of(path);
            Path absPath = configuredPath.isAbsolute()
                    ? configuredPath
                    : Path.of(System.getProperty("user.dir")).resolve(path);
            boolean pathExists = Files.exists(absPath);

            // Structured discovery log (matches [Document Discovery] format used during sync)
            log.info("[Document Discovery] rootPath={}", absPath.toAbsolutePath());
            log.info("[Document Discovery] filesDiscovered={} pathExists={}",
                    discoverable, pathExists);

            log.info("\n" +
                    "╔══════════════════════════════════════════════════════╗\n" +
                    "║              ByteHR AI  —  Startup                  ║\n" +
                    "╠══════════════════════════════════════════════════════╣\n" +
                    "║  Source Type  : {:<36}║\n" +
                    "║  Source Path  : {:<36}║\n" +
                    "║  Abs Path     : {:<36}║\n" +
                    "║  Path Exists  : {:<36}║\n" +
                    "║  Files Found  : {:<36}║\n" +
                    "║  Indexed Docs : {:<36}║\n" +
                    "╚══════════════════════════════════════════════════════╝",
                    type, path,
                    absPath.toAbsolutePath().toString(),
                    pathExists ? "YES" : "NO — check BYTEHR_LOCAL_PATH config",
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
