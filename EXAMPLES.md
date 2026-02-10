# Usage Examples

## Ingest Confluence Page

```bash
# Ingest all pages in space "TEAM"
curl -X POST http://localhost:8080/api/ingest/confluence/TEAM

# Response
{
  "documentsProcessed": 45,
  "chunksProcessed": 234
}
```

## Ingest Jira Project

```bash
# Ingest all issues from project "PROJ"
curl -X POST http://localhost:8080/api/ingest/jira/PROJ

# Response
{
  "documentsProcessed": 120,
  "chunksProcessed": 380
}
```

## Ingest GitHub Repository

```bash
# Configure repositories in .env:
# GITHUB_REPOSITORIES=owner/repo1,owner/repo2

# Then ingest:
curl -X POST http://localhost:8080/api/ingest/github

# Response
{
  "documentsProcessed": 67,
  "chunksProcessed": 189
}
```

## Ingest FileSystem

```bash
# Configure paths in .env:
# FILESYSTEM_BASE_PATHS=/docs,/wiki

# Then ingest:
curl -X POST http://localhost:8080/api/ingest/filesystem

# Response
{
  "documentsProcessed": 23,
  "chunksProcessed": 78
}
```

## Ingest All Sources

```bash
# Ingest from all configured sources
curl -X POST http://localhost:8080/api/ingest/all

# Response
{
  "documentsProcessed": 255,
  "chunksProcessed": 881
}
```

## Query Qdrant (RAG Search)

```bash
# Search for similar content
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How do I request time off?",
    "topK": 5
  }'

# Response
{
  "query": "How do I request time off?",
  "results": [
    {
      "text": "To request time off, log into the HR portal...",
      "source": "Employee Handbook",
      "sourceType": "HR",
      "url": "https://confluence.example.com/pages/12345",
      "score": 0.89
    },
    ...
  ],
  "answer": "To request time off, log into the HR portal..."
}
```

## Query Qdrant Directly (Example)

```python
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, Filter, FieldCondition, MatchValue

# Connect to Qdrant
client = QdrantClient(
    url="https://your-cluster.qdrant.io",
    api_key="your-api-key"
)

# Search for similar vectors
results = client.search(
    collection_name="enterprise-knowledge-base",
    query_vector=embedding_vector,  # 4096-dim vector from bge-m3
    limit=10,
    query_filter=Filter(
        must=[
            FieldCondition(key="source_type", match=MatchValue(value="HR"))
        ]
    )
)

for result in results:
    print(f"Score: {result.score}")
    print(f"Text: {result.payload['text']}")
    print(f"Source: {result.payload['source']}")
```

## Metadata Filtering Examples

### Filter by Department

```python
results = client.search(
    collection_name="enterprise-knowledge-base",
    query_vector=embedding_vector,
    limit=10,
    query_filter=Filter(
        must=[
            FieldCondition(key="department", match=MatchValue(value="Engineering"))
        ]
    )
)
```

### Filter by Security Level

```python
results = client.search(
    collection_name="enterprise-knowledge-base",
    query_vector=embedding_vector,
    limit=10,
    query_filter=Filter(
        must=[
            FieldCondition(key="security_level", match=MatchValue(value="public"))
        ]
    )
)
```

### Filter by Source Type

```python
results = client.search(
    collection_name="enterprise-knowledge-base",
    query_vector=embedding_vector,
    limit=10,
    query_filter=Filter(
        must=[
            FieldCondition(key="source_type", match=MatchValue(value="Confluence"))
        ]
    )
)
```

### Filter by Tags

```python
results = client.search(
    collection_name="enterprise-knowledge-base",
    query_vector=embedding_vector,
    limit=10,
    query_filter=Filter(
        must=[
            FieldCondition(key="tags", match=MatchValue(value="onboarding"))
        ]
    )
)
```

## Scheduled Ingestion

### Using Cron (Linux/Mac)

```bash
# Add to crontab (crontab -e)
# Run daily at 2 AM
0 2 * * * curl -X POST http://localhost:8080/api/ingest/all
```

### Using Kubernetes CronJob

See `DEPLOYMENT.md` for Kubernetes CronJob example.

### Using AWS EventBridge

```json
{
  "Rules": [{
    "Name": "daily-ingestion",
    "ScheduleExpression": "cron(0 2 * * ? *)",
    "State": "ENABLED",
    "Targets": [{
      "Arn": "arn:aws:lambda:region:account:function:trigger-ingestion",
      "Id": "1"
    }]
  }]
}
```

## Batch Processing

The service automatically batches embeddings for efficiency:

- **Chunk Batch Size**: `ingestion.batch-size` (default: 50)
- **Embedding Batch Size**: `ingestion.embed-batch-size` (default: 8)

Adjust these in `application.yml` based on:
- API rate limits
- Memory constraints
- Processing speed requirements

## Error Handling

The service includes robust error handling:

- **Retry Logic**: Automatic retries for transient failures
- **Graceful Degradation**: Continues processing if individual documents fail
- **Logging**: Detailed logs for debugging

Check logs for:
- Failed API calls
- Chunking issues
- Qdrant connection problems

## Performance Tips

1. **Increase Batch Sizes**: For faster processing (if rate limits allow)
2. **Parallel Processing**: Service uses reactive streams for parallelism
3. **Memory**: Increase JVM heap for large documents
4. **Network**: Ensure low latency to Qdrant and Hugging Face APIs

## Monitoring

Monitor these metrics:

- Documents processed per minute
- Chunks created per document
- Embedding API latency
- Qdrant upsert latency
- Error rates

Consider adding Prometheus metrics (see `DEPLOYMENT.md`).
