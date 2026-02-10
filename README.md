# Enterprise Knowledge Base Ingestion Pipeline

Production-ready ingestion service for an organization-level search engine. Reads documents from multiple sources (Confluence, Jira, GitHub, filesystem), chunks them semantically, generates embeddings using **BAAI/bge-m3**, and stores them in Qdrant with rich metadata.

## Features

- **Multi-Source Ingestion**: Confluence, Jira, GitHub, FileSystem
- **Semantic Chunking**: 300-500 tokens per chunk with 50-token overlap
- **BAAI/bge-m3 Embeddings**: Multilingual model with 4096 dimensions
- **Rich Metadata**: Source type, author, department, tags, timestamps, security level
- **Streaming Chat API**: ChatGPT-style streaming responses (mock/ready for LLM)
- **Production-Ready**: Dockerized, environment-driven config, batch processing

## Features

- **Multi-Source Support**: Confluence, Jira, GitHub, and local filesystem
- **Semantic Chunking**: 300-500 tokens per chunk with 50-token overlap
- **BAAI/bge-m3 Embeddings**: Multilingual model with 4096 dimensions
- **Rich Metadata**: Source type, author, department, tags, timestamps, security level, entities
- **Production-Ready**: Dockerized, environment-driven config, batch processing, error handling
- **Platform-Agnostic**: Deployable on-prem, AWS, GCP, Azure, Kubernetes

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│ Confluence  │     │              │     │   Hugging   │     │  Qdrant  │
│    Jira     │────▶│   Chunking   │────▶│    Face     │────▶│  Vector  │
│   GitHub    │     │   Service    │     │ Embeddings  │     │   DB     │
│ FileSystem  │     │              │     │             │     │          │
└─────────────┘     └──────────────┘     └─────────────┘     └──────────┘
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (optional)
- Qdrant instance (Cloud or local)
- Hugging Face API token

### 1. Configuration

Copy `.env.example` to `.env` and configure:

```bash
# Confluence
CONFLUENCE_BASE_URL=https://your-instance.atlassian.net
CONFLUENCE_USERNAME=your-email@example.com
CONFLUENCE_API_TOKEN=your-api-token

# Jira (same instance as Confluence typically)
JIRA_BASE_URL=https://your-instance.atlassian.net
JIRA_USERNAME=your-email@example.com
JIRA_API_TOKEN=your-api-token

# GitHub
GITHUB_API_TOKEN=ghp_your_token
GITHUB_REPOSITORIES=owner/repo1,owner/repo2

# FileSystem (comma-separated paths)
FILESYSTEM_BASE_PATHS=/path/to/docs,/path/to/wiki

# Hugging Face
HUGGINGFACE_API_TOKEN=hf_your_token

# Qdrant
QDRANT_HOST=your-cluster.qdrant.io
QDRANT_PORT=6334
QDRANT_USE_TLS=true
QDRANT_API_KEY=your-api-key
QDRANT_COLLECTION=enterprise-knowledge-base
```

### 2. Build

```bash
mvn clean package -DskipTests
```

### 3. Run Locally

```bash
java -jar target/data-embed-ingestion-service-1.0.0-SNAPSHOT.jar
```

### 4. Docker

```bash
# Build
docker build -t ingestion-service:latest .

# Run
docker run -d \
  --name ingestion-service \
  -p 8080:8080 \
  --env-file .env \
  ingestion-service:latest
```

## API Endpoints

### Chat API (Streaming)
```bash
# Server-Sent Events (SSE)
POST /api/chat
curl -N -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'

# Plain text stream
POST /api/chat/text
curl -N -X POST http://localhost:8080/api/chat/text \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'
```

See [CHAT_API.md](CHAT_API.md) for detailed documentation.

### Ingest All Sources
```bash
POST /api/ingest/all
```

### Ingest Confluence Space
```bash
POST /api/ingest/confluence/{spaceKey}
curl -X POST http://localhost:8080/api/ingest/confluence/TEAM
```

### Ingest Jira Project
```bash
POST /api/ingest/jira/{projectKey}
curl -X POST http://localhost:8080/api/ingest/jira/PROJ
```

### Ingest GitHub Repositories
```bash
POST /api/ingest/github
curl -X POST http://localhost:8080/api/ingest/github
```

### Ingest FileSystem
```bash
POST /api/ingest/filesystem
curl -X POST http://localhost:8080/api/ingest/filesystem
```

### Search (RAG)
```bash
POST /api/search
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the onboarding process?"}'
```

## Metadata Schema

Each vector in Qdrant includes:

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique chunk identifier |
| `text` | string | Original chunk text |
| `source` | string | Document/ticket name |
| `source_type` | string | Confluence, Jira, GitHub, InternalWiki, HR, Finance |
| `url` | string | Link to source document |
| `author` | string | Creator/owner |
| `department` | string | Engineering, Product, HR, Finance, Sales |
| `tags` | array | Keywords/labels |
| `created_at` | string | ISO timestamp |
| `updated_at` | string | ISO timestamp |
| `chunk_id` | int | Position in document (0-based) |
| `embedding_model` | string | BAAI/bge-m3 |
| `security_level` | string | public/internal/confidential |
| `language` | string | en, hi, etc. |
| `precomputed_entities` | array | Important entities (e.g., "Section 80C") |
| `custom_*` | object | Source-specific fields |

## Qdrant Collection

- **Vector Size**: 4096 (BAAI/bge-m3)
- **Distance Metric**: Cosine
- **Collection Name**: `enterprise-knowledge-base` (configurable)

## Examples

### Ingest Confluence Page

```bash
# Ingest all pages in space "TEAM"
curl -X POST http://localhost:8080/api/ingest/confluence/TEAM
```

### Query Qdrant

```bash
# Search for similar content
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "How do I request time off?"}'
```

## Deployment

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingestion-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ingestion-service
  template:
    metadata:
      labels:
        app: ingestion-service
    spec:
      containers:
      - name: ingestion-service
        image: your-registry/ingestion-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: QDRANT_HOST
          valueFrom:
            secretKeyRef:
              name: qdrant-secret
              key: host
        - name: HUGGINGFACE_API_TOKEN
          valueFrom:
            secretKeyRef:
              name: hf-secret
              key: token
```

### Scheduled Ingestion (CronJob)

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ingestion-cron
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: ingestion-service
            image: your-registry/ingestion-service:latest
            command: ["curl", "-X", "POST", "http://localhost:8080/api/ingest/all"]
          restartPolicy: OnFailure
```

## Configuration Reference

See `src/main/resources/application.yml` for all configuration options.

### Environment Variables

All configuration can be overridden via environment variables. See `.env.example` for reference.

## Troubleshooting

### "Not existing vector name" Error

Set `qdrant.recreate-collection: true` in `application.yml` to recreate the collection with correct vector size.

### Embedding API Rate Limits

Adjust `ingestion.embed-batch-size` to control batch size. Reduce if hitting rate limits.

### Large File Processing

Increase `filesystem.max-file-size-kb` or `github.max-file-size-kb` if needed.

## License

MIT
