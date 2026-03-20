package com.example.springai.langchain;

import com.example.springai.langchain.chain.ConversationChain;
import com.example.springai.langchain.chain.LLMChain;
import com.example.springai.langchain.chain.SequentialChain;
import com.example.springai.langchain.model.ChainResult;
import com.example.springai.langchain.model.PromptTemplate;
import com.example.springai.langchain.service.LangChainService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LangChain concepts: {@link PromptTemplate}, {@link LLMChain},
 * {@link ConversationChain}, {@link SequentialChain}, and {@link LangChainService}.
 */
@ExtendWith(MockitoExtension.class)
class LangChainServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private LangChainService langChainService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        langChainService = new LangChainService(chatClientBuilder);
    }

    // -------------------------------------------------------------------------
    // PromptTemplate
    // -------------------------------------------------------------------------

    @Test
    void promptTemplate_shouldReplaceAllPlaceholders() {
        PromptTemplate template = new PromptTemplate("Hello, {name}! You are {age} years old.");
        String result = template.format(Map.of("name", "Alice", "age", "30"));
        assertThat(result).isEqualTo("Hello, Alice! You are 30 years old.");
    }

    @Test
    void promptTemplate_withMissingVariable_shouldThrow() {
        PromptTemplate template = new PromptTemplate("Translate {text} to {language}.");
        assertThatThrownBy(() -> template.format(Map.of("text", "Hello")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unresolved placeholders");
    }

    @Test
    void promptTemplate_withExtraVariable_shouldIgnoreExtra() {
        PromptTemplate template = new PromptTemplate("Hello {name}!");
        String result = template.format(Map.of("name", "Bob", "extra", "ignored"));
        assertThat(result).isEqualTo("Hello Bob!");
    }

    // -------------------------------------------------------------------------
    // LLMChain
    // -------------------------------------------------------------------------

    @Test
    void llmChain_shouldFormatPromptAndCallLLM() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Bonjour le monde");

        ChainResult result = langChainService.runLLMChain(
                "Translate '{text}' to {language}.",
                Map.of("text", "Hello world", "language", "French"));

        assertThat(result.output()).isEqualTo("Bonjour le monde");
        assertThat(result.chainName()).isEqualTo("llm-chain");
    }

    // -------------------------------------------------------------------------
    // ConversationChain
    // -------------------------------------------------------------------------

    @Test
    void conversationChain_shouldMaintainHistoryAcrossMessages() {
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("I'm fine, thanks!")
                .thenReturn("The weather is sunny today.");

        ChainResult first = langChainService.chat("session-1", "Be a helpful assistant.", "How are you?");
        ChainResult second = langChainService.chat("session-1", "Be a helpful assistant.", "What's the weather?");

        assertThat(first.output()).isEqualTo("I'm fine, thanks!");
        assertThat(second.output()).isEqualTo("The weather is sunny today.");
        // Second turn history should include both user messages and first assistant reply
        assertThat(second.intermediates().size()).isGreaterThan(first.intermediates().size());
    }

    @Test
    void clearSession_shouldRemoveConversationHistory() {
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Hello!");

        langChainService.chat("session-2", "You are helpful.", "Hi");
        langChainService.clearSession("session-2");
        // After clearing, a new conversation starts fresh
        ChainResult result = langChainService.chat("session-2", "You are helpful.", "Hi again");
        // The new first turn should have only 2 history entries (user + assistant)
        assertThat(result.intermediates()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // SequentialChain
    // -------------------------------------------------------------------------

    @Test
    void sequentialChain_shouldPassOutputsBetweenSteps() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("Summary of the text.")
                .thenReturn("• Point 1\n• Point 2\n• Point 3");

        ChainResult result = langChainService.runSequentialChain("Long article text here...");

        assertThat(result.output()).contains("Point 1");
        assertThat(result.intermediates()).hasSize(2);
        assertThat(result.intermediates().get(0)).isEqualTo("Summary of the text.");
        // The LLM is called twice (once per chain step)
        verify(chatClient, times(2)).prompt();
    }

    // -------------------------------------------------------------------------
    // Standalone SequentialChain unit tests
    // -------------------------------------------------------------------------

    @Test
    void standaloneSequentialChain_shouldAccumulateIntermediates() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("Step 1 output")
                .thenReturn("Step 2 output");

        LLMChain step1 = new LLMChain("step1", new PromptTemplate("Process: {input}"), chatClient);
        LLMChain step2 = new LLMChain("step2", new PromptTemplate("Refine: {step_1_output}"), chatClient);
        SequentialChain chain = new SequentialChain("test-seq", List.of(step1, step2));

        ChainResult result = chain.run(Map.of("input", "hello"));

        assertThat(result.output()).isEqualTo("Step 2 output");
        assertThat(result.intermediates()).containsExactly("Step 1 output", "Step 2 output");
    }
}
