package com.gen.ai.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

@Service
public class RagEmbeddingsAdminService {

    private static final Logger log = LoggerFactory.getLogger(RagEmbeddingsAdminService.class);

    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    private final ApachePdfBoxDocumentParser pdfParser;
    private final ApacheTikaDocumentParser tikaParser;

    private final Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier;

    // Inject this for direct Chroma calls
    private final RestTemplate restTemplate;

    public RagEmbeddingsAdminService(
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter,
            ApachePdfBoxDocumentParser pdfParser,
            ApacheTikaDocumentParser tikaParser,
            Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier,
            RestTemplate restTemplate) {
        this.embeddingModel = embeddingModel;
        this.documentSplitter = documentSplitter;
        this.pdfParser = pdfParser;
        this.tikaParser = tikaParser;
        this.embeddingStoreSupplier = embeddingStoreSupplier;
        this.restTemplate = restTemplate;
    }

    @Value("${langchain4j.vector-store.chroma.base-url:http://localhost:8000}")
    private String chromaBaseUrl;

    @Value("${langchain4j.vector-store.chroma.collection-name:rag-collection}")
    private String collectionName;

    /**
     * Indexes a document into Chroma vector store (appends chunks).
     * Automatically handles collection recreation if it was deleted.
     */
    public String indexDocument(MultipartFile file, boolean isPdfContent) {
        log.info("Inside indexDocument(MultipartFile file)");

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is null or empty");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
        File tempFile = null;

        try {
            tempFile = File.createTempFile("rag-upload-", fileName);
            file.transferTo(tempFile);

            log.info("Indexing document: {} ({} bytes)", fileName, file.getSize());

            // Step 1: Get a FRESH EmbeddingStore instance (re-resolves UUID of the collection,
            // if the collection was deleted from outside of LangChain4j)
            EmbeddingStore<TextSegment> currentStore = embeddingStoreSupplier.get();

            // Step 2: Load the document and split it into chunks as configured in the application properties file.
            // Apache Tika parser can parse PDF files too; however, we leave the PDF Box Document parser to parse the PDF file.
            Document document;
            if (isPdfContent) {
                document = FileSystemDocumentLoader.loadDocument(tempFile.toPath(), pdfParser);
            } else {
                document = FileSystemDocumentLoader.loadDocument(tempFile.toPath(), tikaParser);
            }

            List<TextSegment> segments = documentSplitter.split(document);

            if (segments.isEmpty()) {
                log.warn("Document parsed but produced no readable chunks: {}", fileName);
                return "File uploaded successfully, but no readable text was found.";
            }

            log.info("Document split into {} chunks. Starting embedding...", segments.size());

            // Step 3: Create the embeddings for each chunk and load the embeddings and chunks into the vector database i.e. Chroma DB
            int embeddedCount = 0;
            for (TextSegment segment : segments) {
                try {
                    Embedding embedding = embeddingModel.embed(segment.text()).content();
                    currentStore.add(embedding, segment);
                    embeddedCount++;
                } catch (Exception e) {
                    log.warn("Failed to embed chunk {} of {} - continuing", embeddedCount + 1, fileName, e);
                }
            }

            // Step 4: It's done and dusted!!!
            log.info("Successfully indexed '{}' → {} chunk(s) stored", fileName, embeddedCount);
            return String.format("Success: '%s' indexed → %d chunk(s) stored", fileName, embeddedCount);

        } catch (IOException e) {
            log.error("I/O error processing file: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read or process the uploaded file");
        } catch (IllegalArgumentException e) {
            log.error("Invalid document format: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported or corrupted file format (PDF expected)");
        } catch (Exception e) {
            log.error("Unexpected error during document indexing", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to index document due to unexpected error");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    log.debug("Temp file deleted: {}", tempFile.getAbsolutePath());
                } catch (Exception ex) {
                    log.warn("Failed to delete temp file (non-critical): {}", tempFile.getAbsolutePath(), ex);
                }
            }
        }
    }

    /**
     * Deletes the entire Chroma collection (full reset of knowledge base).
     */
    public void deleteCollection() {
        log.info("Deleting entire Chroma collection: {}", collectionName);

        try {
            // LangChain4j's EmbeddingStore does NOT have removeAll() kind-of implementation.
            // For Chroma embedding store i.e. vector database, we can either:
            // a) Delete and recreate the collection externally via the Chroma console (most reliable)
            // b) Use direct HTTP DELETE API exposed by Chroma (via the RestTemplate or similar web client)

            // Delete the collection by directly calling Chroma's RESTful API endpoint
            String deleteUrl = chromaBaseUrl + "/api/v1/collections/" + collectionName;
            restTemplate.delete(deleteUrl);

            log.info("Successfully deleted collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to delete Chroma collection: {}", collectionName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete collection: " + e.getMessage());
        }
    }

}
