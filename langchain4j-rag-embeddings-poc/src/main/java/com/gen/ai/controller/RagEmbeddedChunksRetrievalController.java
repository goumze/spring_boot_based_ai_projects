package com.gen.ai.controller;

import com.gen.ai.model.RagRetrievalResponse;
import com.gen.ai.service.RagEmbeddedChunksRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class RagEmbeddedChunksRetrievalController {

    private static final Logger log = LoggerFactory.getLogger(RagEmbeddedChunksRetrievalController.class);

    private final RagEmbeddedChunksRetrievalService retrievalService;

    public RagEmbeddedChunksRetrievalController(RagEmbeddedChunksRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping("/retrieve/embedded-chunks")
    // The retriever has the option to retrieve the Top K results, which is defaulted to 5,
    // if the retriever doesn't mention it in the request
    public ResponseEntity<List<String>> ragQuery(
            @RequestParam(required = true) String question,
            @RequestParam(defaultValue = "5") int topK) {
        log.info("Received RAG JSON query request");

        List<String> response = retrievalService.retrieveEmbeddedChunks(question, topK);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/retrieve/embedded-chunks-with-score")
    // The retriever has the option to retrieve the Top K and Minimum Score qualified results,
    // which are defaulted to 5 and 0.62; respectively, if the retriever doesn't mention them in the request
    public ResponseEntity<RagRetrievalResponse> retrieveChunksWithScore(
            @RequestParam(required = true) String question,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.62") double minScore) {
        log.info("Received RAG JSON query request with scores");

        RagRetrievalResponse response;

        if (question == null || question.trim().isEmpty()) {
            response = RagRetrievalResponse.builder()
                    .question(question)
                    .requestedTopK(topK)
                    .effectiveTopK(0)
                    .minScoreThreshold(minScore)
                    .chunks(List.of())
                    .build();

            return ResponseEntity.badRequest().body(response);
        } else {
            response = retrievalService.retrieveEmbeddedChunksWithScore(question.trim(), topK, minScore);

            return ResponseEntity.ok(response);
        }
    }

}
