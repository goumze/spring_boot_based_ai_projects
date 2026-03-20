package com.gen.ai.controller;

import com.gen.ai.service.RagEmbeddingsAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class RagEmbeddingsAdminController {

    private static final Logger log = LoggerFactory.getLogger(RagEmbeddingsAdminController.class);

    @Value("${langchain4j.vector-store.max-file-size-mbytes:20}")
    private int maxFileSizeInMB;

    private final RagEmbeddingsAdminService ragAdminService;

    public RagEmbeddingsAdminController(RagEmbeddingsAdminService ragAdminService) {
        this.ragAdminService = ragAdminService;
    }

    // For now, allow the processing of .pdf, .txt and .json files.
    // Again, know your limits, and work within those limits.
    // Apache Tika parser, which is already plugged into the application, is capable of parsing the .doc/.docx files too.
    // So, just enable .doc/.docx file upload too.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".txt", ".json");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "text/plain", "application/json");

    @PostMapping(value = "/rag/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadDocument(@RequestPart("file") MultipartFile file) {
        log.info("Received upload request for file: {}", file.getOriginalFilename());

        boolean isPdfContent = false;

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded or file is empty");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        // Validate extension
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
        if (!validExtension) {
            return ResponseEntity.badRequest().body("Unsupported file extension. Allowed: .pdf, .txt, .json");
        }

        // Validate content type
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("Unsupported content type. Allowed: application/pdf, text/plain, application/json");
        } else {
            isPdfContent = "application/pdf".equalsIgnoreCase(contentType);
        }

        // Enforce max size (default 20MB) – configure via 'application.yml'
        if (file.getSize() > (long) maxFileSizeInMB * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File too large (max " + maxFileSizeInMB + "MB)");
        }

        try {
            String result = ragAdminService.indexDocument(file, isPdfContent);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Catch broader exceptions; service throws ResponseStatusException
            log.error("Error during document upload/indexing: {}", fileName, e);
            return ResponseEntity.internalServerError().body("Failed to index document: " + e.getMessage());
        }
    }

    /**
     * Delete the entire Chroma collection (full reset of knowledge base).
     */
    @DeleteMapping("/rag/collection")
    public ResponseEntity<String> deleteCollection() {
        log.info("Received request to delete entire Chroma collection");

        ragAdminService.deleteCollection();

        return ResponseEntity.ok("Collection deleted successfully. Ready for new uploads.");
    }

}
