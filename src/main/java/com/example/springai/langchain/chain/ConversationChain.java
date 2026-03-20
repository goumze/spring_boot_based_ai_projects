package com.example.springai.langchain.chain;

import com.example.springai.langchain.model.ChainResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Conversation Chain with message-level memory.
 *
 * <p>Maintains a rolling window of {@link Message} objects and sends the full conversation history
 * to the LLM on every turn. This mirrors LangChain's {@code ConversationChain} / {@code
 * ConversationBufferMemory} pattern.
 *
 * <p>The conversation is kept in-memory and is scoped to the lifecycle of this bean. For
 * production use, replace the list with a persistent store (e.g., Redis).
 */
public class ConversationChain {

    private final String name;
    private final String systemMessage;
    private final ChatClient chatClient;
    private final List<Message> history;

    public ConversationChain(String name, String systemMessage, ChatClient chatClient) {
        this.name = name;
        this.systemMessage = systemMessage;
        this.chatClient = chatClient;
        this.history = new ArrayList<>();
    }

    /**
     * Sends the next user message and returns the assistant's reply.
     *
     * <p>The full conversation history is included in the request so the LLM can refer to
     * previous turns.
     *
     * @param userInput the next message from the user
     * @return a {@link ChainResult} containing the assistant's reply and the conversation so far
     */
    public ChainResult chat(String userInput) {
        history.add(new UserMessage(userInput));

        Prompt prompt = new Prompt(history);
        String response = chatClient.prompt(prompt)
                .system(systemMessage)
                .call()
                .content();

        history.add(new AssistantMessage(response));

        Map<String, String> input = Map.of("userInput", userInput);
        List<String> historySnapshot = history.stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .toList();
        return new ChainResult(name, input, response, historySnapshot);
    }

    /** Clears the conversation history, starting a fresh session. */
    public void clearHistory() {
        history.clear();
    }

    /** Returns a read-only view of the current conversation history. */
    public List<Message> getHistory() {
        return List.copyOf(history);
    }

    public String getName() {
        return name;
    }
}
