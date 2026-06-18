package com.bytehr.service.impl;

import com.bytehr.config.SourceProperties;
import com.bytehr.model.Document;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.DocumentProcessorService;
import com.bytehr.service.DocumentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "bytehr.source.type", havingValue = "local", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class LocalDocumentSourceServiceImpl implements DocumentSyncService {

    /** Supported document formats — must match Tika-parseable extensions. */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "pptx", "txt", "md");

    private final SourceProperties sourceProperties;
    private final DocumentRepository documentRepository;
    private final DocumentProcessorService documentProcessorService;

    @Override
    @Scheduled(fixedDelayString = "${bytehr.source.sync-interval-ms:3600000}", initialDelay = 5000)
    public int synchronize() {
        Path rootPath = resolveRootPath();

        log.info("[Document Discovery] rootPath={}", rootPath.toAbsolutePath());

        if (!Files.exists(rootPath)) {
            log.warn("[Document Discovery] rootPath does not exist: {}. " +
                     "Verify BYTEHR_LOCAL_PATH is set correctly.", rootPath.toAbsolutePath());
            return 0;
        }

        List<Path> files = scanFiles(rootPath);

        int processed = 0;
        int skipped   = 0;

        for (Path file : files) {
            try {
                int result = processFile(file, rootPath);
                if (result == 1) processed++; else skipped++;
            } catch (Exception e) {
                log.error("[Document Discovery] Failed to process: {}", file.toAbsolutePath(), e);
                skipped++;
            }
        }

        log.info("[Document Discovery] filesDiscovered={} filesProcessed={} filesSkipped={}",
                files.size(), processed, skipped);
        return processed;
    }

    /**
     * Recursively scans for all supported files under rootPath.
     * Logs every discovered file and explicitly skips Windows Zone.Identifier artifacts
     * (files whose name contains ':') which appear when NTFS-sourced files are copied to Linux.
     */
    public List<Path> scanFiles(Path rootPath) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String filename = file.getFileName().toString();

                    // Skip Windows Zone.Identifier ADS artifacts (e.g. "file.pdf:Zone.Identifier")
                    if (filename.contains(":")) {
                        log.debug("[Document Discovery] Skipping Zone.Identifier artifact: {}",
                                file.toAbsolutePath());
                        return FileVisitResult.CONTINUE;
                    }

                    String ext = getExtension(filename);
                    if (SUPPORTED_EXTENSIONS.contains(ext)) {
                        log.info("[Document Discovery] file={}", file.toAbsolutePath());
                        files.add(file);
                    } else {
                        log.debug("[Document Discovery] Skipping unsupported extension '{}': {}",
                                ext, file.getFileName());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("[Document Discovery] Cannot access file: {}", file.toAbsolutePath(), exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("[Document Discovery] Walk failed for rootPath={}: {}",
                    rootPath.toAbsolutePath(), e.getMessage(), e);
        }
        return files;
    }

    /**
     * Processes a single file: new → full ingestion, modified → re-ingest, unchanged → skip.
     *
     * @return 1 if the file was processed (new or modified), 0 if skipped (unchanged)
     */
    private int processFile(Path file, Path rootPath) throws IOException {
        String localId = buildLocalId(file, rootPath);
        Instant fileModified = Files.getLastModifiedTime(file).toInstant();

        Optional<Document> existing = documentRepository.findBySharepointItemId(localId);

        boolean isNew      = existing.isEmpty();
        boolean isModified = existing.isPresent()
                && existing.get().getLastModified() != null
                && fileModified.isAfter(existing.get().getLastModified());

        if (!isNew && !isModified) {
            log.debug("[Document Discovery] Unchanged, skipping: {}", file.getFileName());
            return 0;
        }

        String filename = file.getFileName().toString();
        String ext      = getExtension(filename);
        String country  = detectCountry(file, rootPath);

        Document document = isNew
                ? Document.builder()
                    .name(filename)
                    .sourcePath(file.toAbsolutePath().toString())
                    .sharepointItemId(localId)
                    .fileType(ext)
                    .fileSize(Files.size(file))
                    .lastModified(fileModified)
                    .country(country)
                    .build()
                : existing.get();

        if (!isNew) {
            document.setLastModified(fileModified);
            document.setFileSize(Files.size(file));
        }

        document = documentRepository.save(document);
        byte[] content = Files.readAllBytes(file);
        documentProcessorService.processDocument(document, content);

        log.info("[Document Discovery] {} document: '{}' country={} size={}KB",
                isNew ? "Indexed new" : "Re-indexed modified",
                filename, country != null ? country : "global",
                Files.size(file) / 1024);
        return 1;
    }

    /**
     * Builds a stable unique ID for a local file from its path relative to the root.
     * Format: {@code local://albania-docs/ZTP ENG.pdf}
     */
    private String buildLocalId(Path file, Path rootPath) {
        String relative = rootPath.relativize(file).toString().replace('\\', '/');
        return "local://" + relative;
    }

    /**
     * Infers country from folder path segment (case-insensitive).
     *
     * Supported patterns:
     *   "albania", "albania-docs"          → AL
     *   "serbia", "serbia-docs"            → RS
     *   "sebia",  "sebia-docs"             → RS  (common typo tolerated)
     */
    private String detectCountry(Path file, Path rootPath) {
        String relative = rootPath.relativize(file).toString().toLowerCase().replace('\\', '/');
        if (relative.contains("albania")) return "AL";
        if (relative.contains("serbia") || relative.contains("sebia")) return "RS";
        return null;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private Path resolveRootPath() {
        String raw = sourceProperties.getLocalPath();
        Path p = Path.of(raw);
        return p.isAbsolute() ? p : Path.of(System.getProperty("user.dir")).resolve(p);
    }

    /**
     * Returns the number of supported files currently on disk (for startup validation).
     */
    public int countDiscoverableFiles() {
        Path root = resolveRootPath();
        if (!Files.exists(root)) return 0;
        return scanFiles(root).size();
    }
}
