package com.bytehr.api;

import com.bytehr.model.Document;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.DocumentProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoint for manual document ingestion without SharePoint.
 * Upload PDF, DOCX, XLSX, or PPTX files directly to seed the vector store.
 */
@RestController
@RequestMapping("/api/documents")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentProcessorService documentProcessorService;

    /**
     * Upload one or more HR documents. Each file is processed through the full
     * ingestion pipeline: text extraction → language detection → chunking →
     * embedding generation → vector store persistence.
     *
     * @param files   one or more files (PDF / DOCX / XLSX / PPTX)
     * @param country optional ISO-2 country code (e.g. "AL", "RS") — if omitted,
     *                the document is treated as a generic policy visible to all
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "country", required = false) String country) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No files provided"));
        }

        int processed = 0;
        int failed = 0;
        StringBuilder details = new StringBuilder();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            log.info("Uploading document: {} ({} bytes), country={}", filename, file.getSize(), country);
            try {
                byte[] content = file.getBytes();

                // Create or update the document record (use filename as unique key)
                String itemId = "upload-" + UUID.nameUUIDFromBytes((filename + (country != null ? country : "")).getBytes());
                Document doc = documentRepository.findBySharepointItemId(itemId)
                        .orElseGet(() -> Document.builder()
                                .name(filename)
                                .sourcePath("manual-upload/" + filename)
                                .sharepointItemId(itemId)
                                .fileType(detectType(filename))
                                .fileSize(file.getSize())
                                .lastModified(Instant.now())
                                .lastSync(Instant.now())
                                .country(country != null ? country.toUpperCase() : null)
                                .build());

                doc.setLastSync(Instant.now());
                doc.setFileSize(file.getSize());
                documentRepository.save(doc);

                documentProcessorService.processDocument(doc, content);
                details.append("✔ ").append(filename).append("\n");
                processed++;
            } catch (Exception e) {
                log.error("Failed to process upload: {}", filename, e);
                details.append("✘ ").append(filename).append(": ").append(e.getMessage()).append("\n");
                failed++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "processed", processed,
                "failed", failed,
                "details", details.toString().trim()
        ));
    }

    /**
     * List all ingested documents.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listDocuments() {
        List<Map<String, Object>> docs = documentRepository.findAll().stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "name", d.getName(),
                        "country", d.getCountry() != null ? d.getCountry() : "ALL",
                        "language", d.getLanguage() != null ? d.getLanguage() : "unknown",
                        "chunks", d.getChunks().size(),
                        "lastSync", d.getLastSync() != null ? d.getLastSync().toString() : "never"
                ))
                .toList();
        return ResponseEntity.ok(docs);
    }

    /**
     * Delete a document and all its chunks from the vector store.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable UUID id) {
        if (!documentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted", "id", id.toString()));
    }

    private String detectType(String filename) {
        if (filename == null) return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "docx";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "xlsx";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "pptx";
        return "unknown";
    }
}
