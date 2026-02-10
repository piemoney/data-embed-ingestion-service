# Deployment Guide

## Platform-Agnostic Deployment

This ingestion pipeline can be deployed on any platform that supports Java 17+ or Docker.

## Prerequisites

- Java 17+ or Docker
- Qdrant instance (Cloud or self-hosted)
- Hugging Face API token
- Source system credentials (Confluence, Jira, GitHub, etc.)

## Deployment Options

### 1. Local Development

```bash
# Clone and build
git clone <repo-url>
cd data-embed-ingestion-service
mvn clean package -DskipTests

# Configure .env file
cp .env.example .env
# Edit .env with your credentials

# Run
java -jar target/data-embed-ingestion-service-1.0.0-SNAPSHOT.jar
```

### 2. Docker

```bash
# Build image
docker build -t ingestion-service:latest .

# Run container
docker run -d \
  --name ingestion-service \
  -p 8080:8080 \
  --env-file .env \
  ingestion-service:latest

# View logs
docker logs -f ingestion-service
```

### 3. Docker Compose

```bash
# Start services (includes optional local Qdrant)
docker-compose up -d

# View logs
docker-compose logs -f ingestion-service
```

### 4. Kubernetes

#### Deployment Manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingestion-service
  namespace: knowledge-base
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
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
        env:
        - name: QDRANT_HOST
          valueFrom:
            secretKeyRef:
              name: qdrant-credentials
              key: host
        - name: QDRANT_API_KEY
          valueFrom:
            secretKeyRef:
              name: qdrant-credentials
              key: api-key
        - name: HUGGINGFACE_API_TOKEN
          valueFrom:
            secretKeyRef:
              name: hf-credentials
              key: token
        - name: CONFLUENCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: confluence-credentials
              key: username
        - name: CONFLUENCE_API_TOKEN
          valueFrom:
            secretKeyRef:
              name: confluence-credentials
              key: api-token
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: ingestion-service
  namespace: knowledge-base
spec:
  selector:
    app: ingestion-service
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

#### Secrets

```bash
# Create secrets
kubectl create secret generic qdrant-credentials \
  --from-literal=host=your-cluster.qdrant.io \
  --from-literal=api-key=your-api-key \
  -n knowledge-base

kubectl create secret generic hf-credentials \
  --from-literal=token=hf_your_token \
  -n knowledge-base

kubectl create secret generic confluence-credentials \
  --from-literal=username=your-email@example.com \
  --from-literal=api-token=your-token \
  -n knowledge-base
```

#### Scheduled Ingestion (CronJob)

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ingestion-cron
  namespace: knowledge-base
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM UTC
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: ingestion-service
            image: your-registry/ingestion-service:latest
            command:
            - /bin/sh
            - -c
            - |
              curl -X POST http://ingestion-service:80/api/ingest/all
            env:
            - name: QDRANT_HOST
              valueFrom:
                secretKeyRef:
                  name: qdrant-credentials
                  key: host
            # ... other env vars
          restartPolicy: OnFailure
```

### 5. AWS ECS/Fargate

```json
{
  "family": "ingestion-service",
  "containerDefinitions": [{
    "name": "ingestion-service",
    "image": "your-ecr-repo/ingestion-service:latest",
    "portMappings": [{
      "containerPort": 8080,
      "protocol": "tcp"
    }],
    "environment": [
      {"name": "QDRANT_HOST", "value": "your-cluster.qdrant.io"},
      {"name": "QDRANT_API_KEY", "value": "your-api-key"}
    ],
    "secrets": [
      {"name": "HUGGINGFACE_API_TOKEN", "valueFrom": "arn:aws:secretsmanager:region:account:secret:hf-token"},
      {"name": "CONFLUENCE_API_TOKEN", "valueFrom": "arn:aws:secretsmanager:region:account:secret:confluence-token"}
    ],
    "memory": 2048,
    "cpu": 1024
  }]
}
```

### 6. Google Cloud Run

```bash
# Build and push to GCR
gcloud builds submit --tag gcr.io/PROJECT_ID/ingestion-service

# Deploy
gcloud run deploy ingestion-service \
  --image gcr.io/PROJECT_ID/ingestion-service \
  --platform managed \
  --region us-central1 \
  --set-env-vars QDRANT_HOST=your-cluster.qdrant.io \
  --set-secrets QDRANT_API_KEY=qdrant-api-key:latest \
  --set-secrets HUGGINGFACE_API_TOKEN=hf-token:latest \
  --memory 2Gi \
  --cpu 2 \
  --max-instances 10
```

### 7. Azure Container Instances

```bash
az container create \
  --resource-group myResourceGroup \
  --name ingestion-service \
  --image your-registry/ingestion-service:latest \
  --dns-name-label ingestion-service \
  --ports 8080 \
  --environment-variables \
    QDRANT_HOST=your-cluster.qdrant.io \
    QDRANT_PORT=6334 \
  --secure-environment-variables \
    QDRANT_API_KEY=your-api-key \
    HUGGINGFACE_API_TOKEN=hf_your_token
```

## Health Checks

The service exposes a health endpoint at `/actuator/health` (if Spring Boot Actuator is enabled).

To enable Actuator, add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

And in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

## Monitoring

### Logs

All services log to stdout/stderr. Use your platform's logging solution:

- **Kubernetes**: `kubectl logs -f deployment/ingestion-service`
- **Docker**: `docker logs -f ingestion-service`
- **CloudWatch**: Configure log driver
- **Stackdriver**: Automatic with GCP

### Metrics

Consider adding Prometheus metrics:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

## Scaling

- **Horizontal**: Deploy multiple replicas (stateless service)
- **Vertical**: Increase memory/CPU for batch processing
- **Batch Size**: Adjust `ingestion.embed-batch-size` for rate limits

## Security

1. **Secrets Management**: Use platform secrets (Kubernetes Secrets, AWS Secrets Manager, etc.)
2. **Network**: Restrict access to Qdrant and source APIs via VPC/firewall
3. **TLS**: Enable TLS for Qdrant (`QDRANT_USE_TLS=true`)
4. **API Keys**: Rotate regularly

## Troubleshooting

### Out of Memory

Increase JVM heap:

```bash
java -Xmx2g -jar app.jar
```

Or set in Docker:

```dockerfile
ENV JAVA_OPTS="-Xmx2g"
```

### Rate Limiting

Reduce `ingestion.embed-batch-size` or add retry logic.

### Connection Issues

Check:
- Qdrant host/port accessibility
- TLS settings match Qdrant configuration
- API keys are valid
- Network firewall rules

## Backup & Recovery

- **Qdrant**: Use Qdrant's snapshot/backup features
- **Configuration**: Store in version control (without secrets)
- **State**: Service is stateless; only Qdrant needs backup
