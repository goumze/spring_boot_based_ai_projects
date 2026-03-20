package com.example.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot AI Projects Application
 *
 * <p>This application demonstrates three key AI/LLM concepts using Spring AI:
 *
 * <ul>
 *   <li><b>RAG Pipeline</b> – Retrieval-Augmented Generation: ingest documents, embed them into a
 *       vector store, retrieve relevant chunks, and augment the LLM prompt with context.
 *   <li><b>LangChain concepts</b> – Prompt templates, LLM chains, conversation memory, and
 *       sequential chains.
 *   <li><b>LangGraph concepts</b> – Stateful workflow graphs with typed nodes, conditional edges,
 *       and cyclic execution support.
 * </ul>
 *
 * <p>REST endpoints are available under:
 *
 * <ul>
 *   <li>{@code /api/rag} – RAG pipeline operations
 *   <li>{@code /api/langchain} – LangChain chain operations
 *   <li>{@code /api/langgraph} – LangGraph workflow operations
 * </ul>
 */
@SpringBootApplication
public class SpringAiProjectsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiProjectsApplication.class, args);
    }
}
