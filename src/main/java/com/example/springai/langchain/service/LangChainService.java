package com.example.springai.langchain.service;

import com.example.springai.langchain.chain.ConversationChain;
import com.example.springai.langchain.chain.LLMChain;
import com.example.springai.langchain.chain.SequentialChain;
import com.example.springai.langchain.model.ChainResult;
import com.example.springai.langchain.model.PromptTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * LangChain Concepts Service.
 *
 * <p>Demonstrates the core LangChain abstractions as they would be used in a Spring Boot
 * application:
 *
 * <ul>
 *   <li>{@link PromptTemplate} – named templates with {@code {variable}} placeholders.
 *   <li>{@link LLMChain} – a single prompt-template + LLM call.
 *   <li>{@link ConversationChain} – multi-turn conversation with in-memory history.
 *   <li>{@link SequentialChain} – multiple LLM calls whose outputs are chained together.
 * </ul>
 */
@Service
public class LangChainService {

    private final ChatClient chatClient;

    /** Per-session conversation chains keyed by session ID. */
    private final ConcurrentHashMap<String, ConversationChain> sessions = new ConcurrentHashMap<>();

    public LangChainService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // -------------------------------------------------------------------------
    // LLM Chain
    // -------------------------------------------------------------------------

    /**
     * Runs a single LLM chain using the provided prompt template and input variables.
     *
     * <p>Example: translate a text into a given language.
     *
     * @param templateText the prompt template string with {@code {variable}} placeholders
     * @param variables    the values for each placeholder
     * @return the chain result containing the LLM output
     */
    public ChainResult runLLMChain(String templateText, Map<String, String> variables) {
        PromptTemplate template = new PromptTemplate(templateText);
        LLMChain chain = new LLMChain("llm-chain", template, chatClient);
        return chain.run(variables);
    }

    // -------------------------------------------------------------------------
    // Conversation Chain (with memory)
    // -------------------------------------------------------------------------

    /**
     * Sends a message in a stateful conversation session.
     *
     * <p>If no session exists for the given {@code sessionId}, a new {@link ConversationChain} is
     * created with the provided system prompt. Subsequent calls with the same session ID continue
     * the same conversation.
     *
     * @param sessionId    unique identifier for the conversation session
     * @param systemPrompt the system-level instruction (used only on the first turn)
     * @param userMessage  the user's next message
     * @return the chain result containing the assistant's reply and full history
     */
    public ChainResult chat(String sessionId, String systemPrompt, String userMessage) {
        ConversationChain conversation = sessions.computeIfAbsent(sessionId,
                id -> new ConversationChain("conversation-" + id, systemPrompt, chatClient));
        return conversation.chat(userMessage);
    }

    /**
     * Clears the conversation history for the given session.
     *
     * @param sessionId the session to reset
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    // -------------------------------------------------------------------------
    // Sequential Chain
    // -------------------------------------------------------------------------

    /**
     * Demonstrates a two-step sequential chain:
     *
     * <ol>
     *   <li>Step 1 – Summarise the input {@code text}.
     *   <li>Step 2 – Extract three key bullet points from the summary produced in step 1.
     * </ol>
     *
     * @param text the source text to process
     * @return the chain result with the final output and both intermediate outputs
     */
    public ChainResult runSequentialChain(String text) {
        LLMChain summariseChain = new LLMChain(
                "summarise",
                new PromptTemplate("Summarise the following text in 2-3 sentences:\n\n{text}"),
                chatClient);

        LLMChain bulletPointChain = new LLMChain(
                "bullet-points",
                new PromptTemplate(
                        "Extract exactly 3 key bullet points from this summary:\n\n{step_1_output}"),
                chatClient);

        SequentialChain sequentialChain = new SequentialChain(
                "summarise-and-extract", List.of(summariseChain, bulletPointChain));

        return sequentialChain.run(Map.of("text", text));
    }
}
