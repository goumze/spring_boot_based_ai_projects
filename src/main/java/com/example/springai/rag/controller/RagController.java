package com.example.springai.rag.controller;

import com.example.springai.rag.model.DocumentRequest;
import com.example.springai.rag.model.QueryRequest;
import com.example.springai.rag.model.QueryResponse;
import com.example.springai.rag.service.RagService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the RAG pipeline endpoints.
 *
 * <pre>
 * POST /api/rag/ingest  – ingest a text document into the vector store
 * POST /api/rag/query   – answer a question using the RAG pipeline
 * </pre>
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Ingests a document into the vector store.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "content": "Spring AI is a framework for building AI applications...",
     *   "source": "spring-ai-docs"
     * }
     * }</pre>
     *
     * @param request the document to ingest
     * @return a map containing the number of chunks stored
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody DocumentRequest request) {
        int chunks = ragService.ingestDocument(request);
        return ResponseEntity.ok(Map.of(
                "message", "Document ingested successfully",
                "source", request.source(),
                "chunksStored", chunks));
    }

    /**
     * Answers a question using the RAG pipeline.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "question": "What is Spring AI?",
     *   "topK": 4
     * }
     * }</pre>
     *
     * @param request the question and optional topK parameter
     * @return the generated answer and the sources used
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        QueryResponse response = ragService.query(request.question(), request.topK());
        return ResponseEntity.ok(response);
    }
}
