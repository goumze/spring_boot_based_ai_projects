package com.gen.ai.config;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;
import java.time.Duration;

@Configuration
public class RagEmbeddingsConfig {

    private static final Logger log = LoggerFactory.getLogger(RagEmbeddingsConfig.class);

    @Value("${langchain4j.embeddings.hugging-face.access-token}")
    private String accessToken;

    @Value("${langchain4j.embeddings.hugging-face.model-id:sentence-transformers/all-MiniLM-L6-v2}")
    private String embeddingModelId;

    @Value("${langchain4j.chat-model.ollama.model-name:llama3.2:3b}")
    private String ollamaModel;

    @Value("${langchain4j.chat-model.ollama.base-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${langchain4j.vector-store.chroma.base-url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${langchain4j.vector-store.chroma.collection-name:rag-collection}")
    private String collectionName;

    @Value("${langchain4j.document-parser.chunk-size:200}")
    private int chunkSize;

    @Value("${langchain4j.document-parser.chunk-overlap:20}")
    private int chunkOverlap;

    @Bean
    // In rare scenarios, this bean will be used to invoke the built-in RESTful endpoints exposed by the various
    // components (e.g. Chroma, Ollama etc) directly i.e. by by-passing the LangChain4j
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    // This bean will be used to compute the vectors for a given user query and/or document chunk.
    // HuggingFace (https://huggingface.co/models) provides a seamless interface to the various embedding models,
    // which you can pick n' choose based upon your use case.
    EmbeddingModel embeddingModel() {
        log.info("Creating HuggingFace EmbeddingModel --> model id: {}", embeddingModelId);
        return HuggingFaceEmbeddingModel.builder()
                .accessToken(accessToken)
                .modelId(embeddingModelId)
                .waitForModel(true)
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    // This bean will be used to interact with the underlying LLM via the Ollama service to get answers for the prompts
    public ChatModel chatModel() {
        log.info("Creating Ollama ChatModel --> {} | {}", ollamaUrl, ollamaModel);
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaModel)
                .temperature(0.7)
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean
    @Lazy
    // This is an expensive dependency (e.g. external services like Chroma, Ollama, databases).
    // Bean might fail to initialize (e.g. Chroma not running yet) during application start-up,
    // which prevents application from starting cleanly.
    // With @Lazy, the bean is NOT created during application startup. Only instantiated the first time something
    // injects or requests it (e.g. when controller/service first calls embeddingStore).
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Creating Chroma EmbeddingStore (Chroma 0.5+ compatible) --> {} | collection: {}", chromaUrl, collectionName);
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(collectionName)
                .apiVersion(ChromaApiVersion.V1)
                .build();
    }

    @Bean
    // Internally, when embeddingStore, which represents Chroma vector database instance, is created,
    // a UUID is assigned to the Chroma vector database collection, which is cached by LangChain4j interface.
    // When the Chroma vector database collection is deleted from outside of LangChain4j's scope i.e. either via
    // Chroma vector database web console or via its RESTful API endpoint, the cached UUID doesn't get cleared.
    // As such, any subsequent calls to the Chroma vector database via LangChain4j will throw 'Collection not found'
    // exception, as it still holds the old UUID cached within its scope.
    // So, the below embeddingStoreSupplier() bean is leveraged to recreate a new bean, at least, during create and
    // delete steps, which resolves the latest UUID always.
    // You might still want to continue using the above embeddingStore() bean for retrieving embeddings and chunks
    // from the Chroma vector database.
    public Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier() {
        return () -> {
            log.info("Creating fresh ChromaEmbeddingStore instance for collection: {}", collectionName);
            return ChromaEmbeddingStore.builder()
                    .baseUrl(chromaUrl)
                    .collectionName(collectionName)
                    .apiVersion(ChromaApiVersion.V1)
                    .build();
        };
    }

    @Bean
    // The bean to split the text content read from a document (e.g. PDF document) into chunks.
    // Chunk size determines how much text is captured per embedding. Chunk overlap ensures smooth transitions
    // and avoids losing meaning at boundaries.
    // Together, the Chunk size and Chunk overlap parameters optimize retrieval accuracy and context integrity
    // for embedding-based applications.
    // A Practical Tip: Start with chunks containing 500 tokens + 50 overlap, as a baseline.
    // Keep the overlap ≈ 10–20% of chunk size.
    public DocumentSplitter documentSplitter() {
        log.info("Creating DocumentSplitter --> chunkSize: {}, overlap: {}", chunkSize, chunkOverlap);
        return DocumentSplitters.recursive(chunkSize, chunkOverlap);
    }

    @Bean
    // The bean to parse and extract content of a PDF document
    public ApachePdfBoxDocumentParser pdfParser() {
        log.info("Creating DocumentParser --> pdfParser: ApachePdfBoxDocumentParser");
        return new ApachePdfBoxDocumentParser();
    }

    @Bean
    // The bean to parse and extract content of documents in various formats (e.g. .txt, .json, .docx)
    public ApacheTikaDocumentParser tikaParser() {
        log.info("Creating DocumentParser --> tikaParser: ApacheTikaDocumentParser");
        return new ApacheTikaDocumentParser();
    }

}
