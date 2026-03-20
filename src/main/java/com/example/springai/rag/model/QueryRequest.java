package com.example.springai.rag.model;

/**
 * Request body for querying the RAG pipeline.
 *
 * @param question the natural-language question to answer
 * @param topK     the maximum number of document chunks to retrieve (default 4)
 */
public record QueryRequest(String question, int topK) {

    /** Constructs a {@link QueryRequest} with the default {@code topK} value of 4. */
    public QueryRequest(String question) {
        this(question, 4);
    }
}
