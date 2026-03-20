package com.example.springai.rag.model;

/**
 * Request body for ingesting a text document into the RAG vector store.
 *
 * @param content the raw text content to ingest
 * @param source  a human-readable label identifying the origin of the document (e.g. filename)
 */
public record DocumentRequest(String content, String source) {}
