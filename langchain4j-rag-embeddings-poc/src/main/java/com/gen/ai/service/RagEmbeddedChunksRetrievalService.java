package com.gen.ai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gen.ai.model.RagRetrievalResponse;
import com.gen.ai.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RagEmbeddedChunksRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagEmbeddedChunksRetrievalService.class);

    private final EmbeddingModel embeddingModel;
    private final Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier;

    public RagEmbeddedChunksRetrievalService(
            EmbeddingModel embeddingModel,
            Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier) {
        this.embeddingModel = embeddingModel;
        this.embeddingStoreSupplier = embeddingStoreSupplier;
    }

    @Value("${langchain4j.vector-store.chroma.top-k-max:20}")
    private int topKMax;

    @Value("${langchain4j.vector-store.chroma.default-min-score-threshold:0.7}")
    private double minScoreThreshold;

    private List<EmbeddingMatch<TextSegment>> retrieveRankedSearches(String question, int requestedTopK, double minScoreThreshold) {
        log.info("Retrieving chunks with scores | q='{}' | topK={} | minScore={} now", question, requestedTopK, minScoreThreshold);

        // Step 1: Embed the query once
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        // Step 2: Get fresh store instance (your existing pattern)
        EmbeddingStore<TextSegment> store = embeddingStoreSupplier.get();

        // Step 3: Retrieve matches with scores (lower-level API). It's always good to know your limits and work within those limits, so..
        int fetchLimit = Math.min(requestedTopK * 2, topKMax * 2); // Over-fetch a bit for safety

        // Chroma uses 'minScoreThreshold' as minimum similarity threshold
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .minScore(minScoreThreshold)
                .maxResults(fetchLimit)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();

        // Step 4: Re-rank by score descending (stable sort — already sorted by most stores, but explicit is safer)
        List<EmbeddingMatch<TextSegment>> ranked = matches.stream()
                .sorted(Comparator.comparingDouble(EmbeddingMatch<TextSegment>::score).reversed())
                .limit(requestedTopK)
                .toList();

        return ranked;
    }

    // This service method corresponds to the GET endpoint '/retrieve/embedded-chunks'
    public List<String> retrieveEmbeddedChunks(String question, int requestedTopK) {
        log.info("Retrieving chunks with scores | q='{}' | topK={} | minScore={}", question, requestedTopK, minScoreThreshold);

        try {
            // Step 1: Retrieve ranked results sorted by score in descending order
            // (stable sort — already sorted by most stores, but explicit is safer)
            List<EmbeddingMatch<TextSegment>> rankedResults = retrieveRankedSearches(question, requestedTopK, minScoreThreshold);

            if ((rankedResults == null) || (rankedResults.isEmpty())) {
                return List.of();
            }

            // Step 2: Collect the document chunks i.e. knowledge fetched from the embedding store
            // (i.e. grounded truths) and return
            List<String> chunks = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> content : rankedResults) {
                chunks.add(content.embedded().text());
            }
            return chunks;

        } catch (Exception e) {
            log.error("Failed to process JSON RAG query: {}", question, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate JSON response");
        }
    }

    // This service method corresponds to the GET endpoint '/retrieve/embedded-chunks-with-score'
    public RagRetrievalResponse retrieveEmbeddedChunksWithScore(String question, int requestedTopK, double minScoreThreshold) {
        log.info("Retrieving chunks with scores | q='{}' | topK={} | minScore={}", question, requestedTopK, minScoreThreshold);

        try {
            // Step 1: Retrieve ranked results sorted by score in descending order
            // (stable sort — already sorted by most stores, but explicit is safer)
            List<EmbeddingMatch<TextSegment>> rankedResults = retrieveRankedSearches(question, requestedTopK, minScoreThreshold);

            // Step 2: Build rich DTOs with rank
            List<RetrievedChunk> chunks = IntStream.range(0, rankedResults.size())
                    .mapToObj(i -> {
                        EmbeddingMatch<TextSegment> m = rankedResults.get(i);
                        TextSegment seg = m.embedded();
                        // score (m.score()) --> cosine similarity (usually 0..1)
                        // rank (i + 1)     --> human-friendly rank: 1 = best
                        return new RetrievedChunk(seg.text(), m.score(), i + 1, seg.metadata().toMap(), seg.text().length());
                    })
                    .collect(Collectors.toList());

            // Step 3: Build final response
            return RagRetrievalResponse.builder()
                    .question(question)
                    .requestedTopK(requestedTopK)
                    .effectiveTopK(chunks.size())
                    .minScoreThreshold(minScoreThreshold)
                    .chunks(chunks)
                    .build();

        } catch (Exception e) {
            log.error("Failed to retrieve chunks with scores for question: {}", question, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve relevant chunks");
        }
    }

}
