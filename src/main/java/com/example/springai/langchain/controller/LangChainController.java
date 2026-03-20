package com.example.springai.langchain.controller;

import com.example.springai.langchain.model.ChainResult;
import com.example.springai.langchain.service.LangChainService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing LangChain concept endpoints.
 *
 * <pre>
 * POST   /api/langchain/llm-chain                  – run a single LLM chain
 * POST   /api/langchain/conversation/{sessionId}   – send a message in a conversation session
 * DELETE /api/langchain/conversation/{sessionId}   – clear conversation history
 * POST   /api/langchain/sequential-chain           – run a sequential (multi-step) chain
 * </pre>
 */
@RestController
@RequestMapping("/api/langchain")
public class LangChainController {

    private final LangChainService langChainService;

    public LangChainController(LangChainService langChainService) {
        this.langChainService = langChainService;
    }

    /**
     * Runs a single LLM chain with a user-supplied template and variables.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "template": "Translate '{text}' to {language}.",
     *   "variables": { "text": "Hello world", "language": "French" }
     * }
     * }</pre>
     */
    @PostMapping("/llm-chain")
    public ResponseEntity<ChainResult> runLLMChain(@RequestBody LLMChainRequest request) {
        ChainResult result = langChainService.runLLMChain(request.template(), request.variables());
        return ResponseEntity.ok(result);
    }

    /**
     * Sends a user message in a stateful conversation session.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "systemPrompt": "You are a helpful travel advisor.",
     *   "message": "What should I pack for a trip to Japan?"
     * }
     * }</pre>
     */
    @PostMapping("/conversation/{sessionId}")
    public ResponseEntity<ChainResult> chat(
            @PathVariable String sessionId,
            @RequestBody ConversationRequest request) {
        ChainResult result = langChainService.chat(sessionId, request.systemPrompt(), request.message());
        return ResponseEntity.ok(result);
    }

    /**
     * Clears the conversation history for the given session ID.
     */
    @DeleteMapping("/conversation/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        langChainService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session " + sessionId + " cleared"));
    }

    /**
     * Runs a two-step sequential chain: summarise → extract bullet points.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "text": "Long article text here..."
     * }
     * }</pre>
     */
    @PostMapping("/sequential-chain")
    public ResponseEntity<ChainResult> runSequentialChain(@RequestBody TextRequest request) {
        ChainResult result = langChainService.runSequentialChain(request.text());
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Inline request records
    // -------------------------------------------------------------------------

    record LLMChainRequest(String template, Map<String, String> variables) {}

    record ConversationRequest(String systemPrompt, String message) {}

    record TextRequest(String text) {}
}
