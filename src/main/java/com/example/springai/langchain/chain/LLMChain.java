package com.example.springai.langchain.chain;

import com.example.springai.langchain.model.ChainResult;
import com.example.springai.langchain.model.PromptTemplate;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;

/**
 * LLM Chain – the most fundamental LangChain abstraction.
 *
 * <p>An {@link LLMChain} pairs a {@link PromptTemplate} with an LLM call. It:
 * <ol>
 *   <li>Formats the prompt template with the supplied variables.
 *   <li>Sends the resolved prompt to the LLM.
 *   <li>Returns the LLM response wrapped in a {@link ChainResult}.
 * </ol>
 *
 * <p>This mirrors LangChain's {@code LLMChain} pattern.
 */
public class LLMChain {

    private final String name;
    private final PromptTemplate promptTemplate;
    private final ChatClient chatClient;

    public LLMChain(String name, PromptTemplate promptTemplate, ChatClient chatClient) {
        this.name = name;
        this.promptTemplate = promptTemplate;
        this.chatClient = chatClient;
    }

    /**
     * Executes the chain with the provided input variables.
     *
     * @param input a map of template variable name to value
     * @return a {@link ChainResult} containing the LLM output
     */
    public ChainResult run(Map<String, String> input) {
        String resolvedPrompt = promptTemplate.format(input);
        String output = chatClient.prompt()
                .user(resolvedPrompt)
                .call()
                .content();
        return ChainResult.of(name, input, output);
    }

    public String getName() {
        return name;
    }
}
