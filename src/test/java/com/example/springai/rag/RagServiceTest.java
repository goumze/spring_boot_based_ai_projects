package com.example.springai.rag;

import com.example.springai.rag.model.DocumentRequest;
import com.example.springai.rag.model.QueryResponse;
import com.example.springai.rag.service.RagService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RagService}.
 *
 * <p>All external dependencies (ChatClient, VectorStore) are mocked with Mockito so tests run
 * without real API credentials.
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private VectorStore vectorStore;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ragService = new RagService(chatClientBuilder, vectorStore);
    }

    @Test
    void ingestDocument_shouldSplitAndAddToVectorStore() {
        DocumentRequest request = new DocumentRequest(
                "Spring AI is a framework for building AI applications in Java.",
                "spring-ai-docs");

        int chunks = ragService.ingestDocument(request);

        assertThat(chunks).isGreaterThan(0);
        verify(vectorStore).add(anyList());
    }

    @Test
    void query_shouldRetrieveDocumentsAndCallLLM() {
        Document doc = new Document("Spring AI supports OpenAI, Anthropic, and more.",
                Map.of("source", "spring-ai-docs"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Spring AI is a framework by Pivotal.");

        QueryResponse response = ragService.query("What is Spring AI?", 4);

        assertThat(response.question()).isEqualTo("What is Spring AI?");
        assertThat(response.answer()).isEqualTo("Spring AI is a framework by Pivotal.");
        assertThat(response.sourcesUsed()).contains("spring-ai-docs");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void query_withNoMatchingDocuments_shouldReturnAnswerWithEmptyContext() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("I don't have enough information to answer.");

        QueryResponse response = ragService.query("Unknown topic", 4);

        assertThat(response.sourcesUsed()).isEmpty();
        assertThat(response.answer()).contains("I don't have enough information");
    }
}
