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

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md");

    private final SourceProperties sourceProperties;
    private final DocumentRepository documentRepository;
    private final DocumentProcessorService documentProcessorService;

    @Override
    @Scheduled(fixedDelayString = "${bytehr.source.sync-interval-ms:3600000}", initialDelay = 5000)
    public int synchronize() {
        Path rootPath = resolveRootPath();
        log.info("Starting local document synchronization from: {}", rootPath.toAbsolutePath());

        if (!Files.exists(rootPath)) {
            log.warn("Local document path does not exist: {}. Skipping sync.", rootPath.toAbsolutePath());
            return 0;
        }

        List<Path> files = scanFiles(rootPath);
        log.info("Found {} supported document(s) in {}", files.size(), rootPath.toAbsolutePath());

        int processed = 0;
        for (Path file : files) {
            try {
                processed += processFile(file, rootPath);
            } catch (Exception e) {
                log.error("Failed to process local file: {}", file, e);
            }
        }

        log.info("Local synchronization complete. Documents processed: {}", processed);
        return processed;
    }

    /**
     * Returns all supported files found recursively under the given root.
     */
    public List<Path> scanFiles(Path rootPath) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String ext = getExtension(file.getFileName().toString());
                    if (SUPPORTED_EXTENSIONS.contains(ext)) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Cannot access file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to scan local document path: {}", rootPath, e);
        }
        return files;
    }

    /**
     * Processes a single file: detects new vs modified and runs the ingestion pipeline.
     *
     * @return 1 if the file was processed, 0 if it was skipped (already indexed + unchanged)
     */
    private int processFile(Path file, Path rootPath) throws IOException {
        String localId = buildLocalId(file, rootPath);
        Instant fileModified = Files.getLastModifiedTime(file).toInstant();

        Optional<Document> existing = documentRepository.findBySharepointItemId(localId);

        boolean isNew = existing.isEmpty();
        boolean isModified = existing.isPresent()
                && existing.get().getLastModified() != null
                && fileModified.isAfter(existing.get().getLastModified());

        if (!isNew && !isModified) {
            log.debug("Skipping unchanged file: {}", file.getFileName());
            return 0;
        }

        String filename = file.getFileName().toString();
        String ext = getExtension(filename);
        String country = detectCountry(file, rootPath);

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

        log.info("{} local document: {} (country={})", isNew ? "Indexed new" : "Re-indexed modified",
                filename, country != null ? country : "global");
        return 1;
    }

    /**
     * Builds a stable unique ID for a local file from its path relative to the root.
     * Format: {@code local://albania/vacation-policy.md}
     */
    private String buildLocalId(Path file, Path rootPath) {
        String relative = rootPath.relativize(file).toString().replace('\\', '/');
        return "local://" + relative;
    }

    /**
     * Infers a two-letter ISO country code from the path of the file.
     * Convention: files under an "albania" folder → "AL", "serbia" → "RS".
     */
    private String detectCountry(Path file, Path rootPath) {
        String relative = rootPath.relativize(file).toString().toLowerCase().replace('\\', '/');
        if (relative.contains("albania")) return "AL";
        if (relative.contains("serbia")) return "RS";
        return null;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private Path resolveRootPath() {
        String raw = sourceProperties.getLocalPath();
        Path p = Path.of(raw);
        // Resolve relative paths against the current working directory
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
