# GenAI — Building RAG Pipeline with Java + SpringBoot

A fully working **RAG (Retrieval-Augmented Generation) Pipeline** built with **Java 21 + Spring Boot 3.3 + LangChain4j**.

Based on the article: [GenAI — Building RAG Pipeline with Java + SpringBoot](https://medium.com/@amitsriv99/genai-building-rag-pipeline-with-java-springboot-7bf2d6eb9e76)

## Business Use Case — Tours & Travels Guide

This RAG pipeline fills the knowledge gaps in an underlying LLM by providing additional context about pilgrimage and tourist sites across India — including up-to-date expenses per person, accessibility details, and nearby attractions.

## Architecture

```
PDF/TXT Document
      |
      v
[Apache PDFBox / Tika Parser]   ← Step 1: Parse document
      |
      v
[Document Splitter]              ← Step 2: Split into chunks (configurable size + overlap)
      |
      v
[HuggingFace Embedding Model]   ← Step 3: Generate embedding vectors for each chunk
      |
      v
[Chroma Vector Store]           ← Step 4: Persist embeddings & chunks

User Query
      |
      v
[HuggingFace Embedding Model]   ← Embed the query
      |
      v
[Chroma Vector Store]           ← Cosine similarity search → Top K chunks
      |
      v
REST API Response               ← Return relevant chunks (with optional scores/ranks)
```

## Tech Stack

| Component              | Technology                                     |
|------------------------|------------------------------------------------|
| Framework              | Spring Boot 3.3.5, Java 21                     |
| AI / LLM               | LangChain4j 1.0.0-beta3, Ollama (llama3.2:3b)  |
| Embedding Model        | HuggingFace `sentence-transformers/all-MiniLM-L6-v2` |
| Vector Store           | Chroma DB 0.5.4                                |
| Document Parsing       | Apache PDFBox, Apache Tika                     |
| Build Tool             | Maven                                          |

## Prerequisites

1. **Java 21** installed
2. **Maven** installed
3. **Docker** installed (for Chroma)
4. **Ollama** installed and running locally
5. **HuggingFace access token** — generate at https://huggingface.co/settings/tokens

## Setup & Run

### Step 1: Start Chroma Vector Database

```bash
# Using docker-compose (recommended)
docker-compose up -d

# Or directly with Docker
docker run -d -p 8000:8000 --name chroma -v chroma-data:/chroma/chroma chromadb/chroma:0.5.4
```

Verify Chroma is running:
```bash
curl http://localhost:8000/api/v1/version
# Output: {"version":"0.5.4"}
```

### Step 2: Set Up Ollama and Pull the LLM

```bash
# Start Ollama service
nohup ollama serve > ollama.log 2>&1 &
echo $! > ollama.pid

# Pull llama3.2:3b model
ollama pull llama3.2:3b

# Verify
curl http://localhost:11434/api/tags
```

### Step 3: Set HuggingFace Token

```bash
export HUGGING_FACE_TOKEN=<your_huggingface_access_token>
```

### Step 4: Build and Run the Application

```bash
# Build
mvn clean install

# Run with default chunk size (from application.yml)
nohup java -jar target/rag-embeddings-poc-1.0.0.jar > rag.log 2>&1 &
echo $! > rag.pid

# Run with custom chunk size/overlap overrides
nohup java -Dlangchain4j.document-parser.chunk-size=500 \
           -Dlangchain4j.document-parser.chunk-overlap=50 \
           -jar target/rag-embeddings-poc-1.0.0.jar > rag.log 2>&1 &
echo $! > rag.pid

# Stop the application
kill $(cat rag.pid)
```

The application starts at: `http://localhost:8080/langchain4j-rag-embeddings-poc/`

## REST API Endpoints

### Admin Endpoints (Document Management)

#### Upload & Index a Document
```bash
curl --location 'http://localhost:8080/langchain4j-rag-embeddings-poc/api/admin/rag/upload' \
     --form 'file=@"/path/to/your/document.pdf"'
```

#### Delete the Entire Chroma Collection (Hard Reset)
```bash
curl --location --request DELETE \
     'http://localhost:8080/langchain4j-rag-embeddings-poc/api/admin/rag/collection'
```

### Retrieval Endpoints

#### Retrieve Top K Relevant Chunks (Simple)
```bash
curl --location 'http://localhost:8080/langchain4j-rag-embeddings-poc/api/v1/retrieve/embedded-chunks?question=Jyotirlingas%20in%20south%20india&topK=5'
```

**Sample Response:**
```json
[
  "3. Rameswaram Ramanathaswamy Temple, Tamil Nadu \n Deity: Lord Shiva (Jyotirlinga) ...",
  "10. Somnath Temple, Gujarat \n Deity: Lord Shiva (Jyotirlinga) ..."
]
```

#### Retrieve Top K Chunks with Scores and Metadata
```bash
curl --location 'http://localhost:8080/langchain4j-rag-embeddings-poc/api/v1/retrieve/embedded-chunks-with-score?question=Jyotirlingas%20in%20south%20india&topK=5&minScore=0.7'
```

**Sample Response:**
```json
{
  "question": "Jyotirlingas in south india",
  "requestedTopK": 5,
  "effectiveTopK": 3,
  "minScoreThreshold": 0.7,
  "chunks": [
    {
      "text": "3. Rameswaram Ramanathaswamy Temple ...",
      "score": 0.7519,
      "rank": 1,
      "metadata": { "file_name": "...", "index": "3" },
      "textLength": 357
    }
  ]
}
```

## Chroma REST API (Direct Queries)

```bash
# Check Chroma version
curl http://localhost:8000/api/v1/version

# List all collections
curl http://localhost:8000/api/v1/collections

# View collection info
curl http://localhost:8000/api/v1/collections/tourists-pilgrimages-sites-collection-v10

# Fetch all chunks and embeddings in a collection
curl --location 'http://localhost:8000/api/v1/collections/<collection-uuid>/get' \
     --header 'Content-Type: application/json' \
     --data '{"include": ["documents", "metadatas", "embeddings"]}'
```

## Project Structure

```
langchain4j-rag-embeddings-poc/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        ├── java/
        │   └── com/gen/ai/
        │       ├── RagEmbeddingsPocApplication.java
        │       ├── config/
        │       │   └── RagEmbeddingsConfig.java
        │       ├── controller/
        │       │   ├── RagEmbeddingsAdminController.java
        │       │   └── RagEmbeddedChunksRetrievalController.java
        │       ├── service/
        │       │   ├── RagEmbeddingsAdminService.java
        │       │   └── RagEmbeddedChunksRetrievalService.java
        │       └── model/
        │           ├── RetrievedChunk.java
        │           └── RagRetrievalResponse.java
        └── resources/
            └── application.yml
```

## Configuration Reference

Key configuration properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `langchain4j.embeddings.hugging-face.model-id` | `sentence-transformers/all-MiniLM-L6-v2` | HuggingFace embedding model |
| `langchain4j.vector-store.chroma.base-url` | `http://localhost:8000` | Chroma DB URL |
| `langchain4j.vector-store.chroma.collection-name` | `tourists-pilgrimages-sites-collection-v10` | Collection name |
| `langchain4j.vector-store.chroma.top-k-max` | `20` | Maximum retrieval limit |
| `langchain4j.vector-store.chroma.default-min-score-threshold` | `0.62` | Minimum cosine similarity |
| `langchain4j.chat-model.ollama.model-name` | `llama3.2:3b` | Ollama LLM model |
| `langchain4j.document-parser.chunk-size` | `1000` | Chunk size in tokens |
| `langchain4j.document-parser.chunk-overlap` | `200` | Overlap between chunks |

## Security Note

> The admin endpoints `/api/admin/rag/upload` and `/api/admin/rag/collection` should be protected with RBAC/OAuth in production. This is intentionally left as an exercise.
