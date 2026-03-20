package com.example.springai.rag.model;

import java.util.List;

/**
 * Response returned by the RAG query endpoint.
 *
 * @param question      the original question
 * @param answer        the LLM-generated answer augmented with retrieved context
 * @param sourcesUsed   the source labels of the document chunks used as context
 */
public record QueryResponse(String question, String answer, List<String> sourcesUsed) {}
