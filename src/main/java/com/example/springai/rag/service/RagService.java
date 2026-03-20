package com.example.springai.rag.service;

import com.example.springai.rag.model.DocumentRequest;
import com.example.springai.rag.model.QueryResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * RAG (Retrieval-Augmented Generation) Pipeline Service.
 *
 * <p>This service demonstrates the three stages of a RAG pipeline:
 *
 * <ol>
 *   <li><b>Ingestion</b> – raw text is split into chunks and embedded into a {@link VectorStore}.
 *   <li><b>Retrieval</b> – at query time the top-K most semantically similar chunks are fetched.
 *   <li><b>Generation</b> – the retrieved chunks are prepended to the LLM prompt as context,
 *       grounding the answer in the ingested knowledge base.
 * </ol>
 */
@Service
public class RagService {

    private static final String SYSTEM_PROMPT_TEMPLATE =
            """
            You are a helpful assistant. Answer the user's question using ONLY the information
            provided in the context below. If the context does not contain enough information,
            say so clearly.

            Context:
            %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter();
    }

    /**
     * Ingests a document into the vector store.
     *
     * <p>The content is split into overlapping token-based chunks before embedding so that large
     * documents can be retrieved at fine-grained granularity.
     *
     * @param request the document content and source label
     * @return the number of chunks stored
     */
    public int ingestDocument(DocumentRequest request) {
        Document rawDocument = new Document(request.content(),
                Map.of("source", request.source()));
        List<Document> chunks = textSplitter.apply(List.of(rawDocument));
        vectorStore.add(chunks);
        return chunks.size();
    }

    /**
     * Answers a question using the RAG pipeline.
     *
     * <p>Retrieves the {@code topK} most relevant document chunks, builds a context string, and
     * passes it together with the question to the LLM.
     *
     * @param question the natural-language question
     * @param topK     number of chunks to retrieve
     * @return a {@link QueryResponse} containing the answer and the sources used
     */
    public QueryResponse query(String question, int topK) {
        // Stage 1 – Retrieve relevant chunks from the vector store
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .build();
        List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

        // Stage 2 – Build context from retrieved chunks
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        List<String> sources = relevantDocs.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "unknown"))
                .distinct()
                .collect(Collectors.toList());

        // Stage 3 – Generate answer grounded in the retrieved context
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        return new QueryResponse(question, answer, sources);
    }
}
