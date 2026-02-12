# Logging Guide

This document explains where to find logs for the data-embed-ingestion-service in different deployment scenarios.

## Default Behavior

By default, Spring Boot logs to **stdout/stderr** (console). Logs are formatted with timestamps, thread names, log levels, and messages.

## Where to See Logs

### 1. **Local Development (Running JAR)**

When running locally:
```bash
java -jar target/data-embed-ingestion-service-1.0.0-SNAPSHOT.jar
```

**Logs appear in:**
- **Console/Terminal** - All logs are printed to stdout/stderr
- **`logs/application.log`** - If file logging is enabled in `application.yml`

**View logs:**
```bash
# Follow logs in real-time
tail -f logs/application.log

# Or if running in terminal, logs appear directly in the console
```

---

### 2. **Docker**

When running in Docker:
```bash
docker run -d --name ingestion-service -p 8080:8080 ingestion-service:latest
```

**Logs appear in:**
- **Docker logs** - Use `docker logs` command
- **Container stdout/stderr** - Captured by Docker

**View logs:**
```bash
# View all logs
docker logs ingestion-service

# Follow logs in real-time
docker logs -f ingestion-service

# View last 100 lines
docker logs --tail 100 ingestion-service

# View logs with timestamps
docker logs -t ingestion-service
```

---

### 3. **Docker Compose**

When using docker-compose:
```bash
docker-compose up -d
```

**Logs appear in:**
- **docker-compose logs** command

**View logs:**
```bash
# View all service logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View logs for specific service
docker-compose logs -f ingestion-service

# View last 100 lines
docker-compose logs --tail 100
```

---

### 4. **AWS Elastic Beanstalk**

When deployed to Elastic Beanstalk:

**Logs appear in:**
- **AWS CloudWatch Logs** - Primary location
- **EB CLI** - Can stream logs locally
- **EB Console** - View logs in browser

**View logs:**

**Option A: AWS Console**
1. Go to [Elastic Beanstalk Console](https://console.aws.amazon.com/elasticbeanstalk)
2. Select your environment
3. Click **"Logs"** in the left sidebar
4. Click **"Request Logs"** → **"Last 100 Lines"** or **"Last Hour"**
5. Click **"Download"** to view logs

**Option B: EB CLI**
```bash
# Stream logs in real-time
eb logs

# Download logs
eb logs --all

# View logs for specific instance
eb logs --instance-id i-xxxxx
```

**Option C: CloudWatch Logs Console**
1. Go to [CloudWatch Console](https://console.aws.amazon.com/cloudwatch)
2. Click **"Logs"** → **"Log groups"**
3. Find log group: `/aws/elasticbeanstalk/{environment-name}/var/log/eb-engine.log`
4. Click on the log stream to view logs

**Option D: AWS CLI**
```bash
# List log groups
aws logs describe-log-groups --region ap-south-1

# Stream logs
aws logs tail /aws/elasticbeanstalk/data-embed-ingestion-service-env/var/log/eb-engine.log --follow --region ap-south-1

# Get recent log events
aws logs get-log-events \
  --log-group-name /aws/elasticbeanstalk/data-embed-ingestion-service-env/var/log/eb-engine.log \
  --region ap-south-1 \
  --limit 100
```

---

### 5. **AWS ECS / Fargate**

When deployed to ECS:

**Logs appear in:**
- **AWS CloudWatch Logs** - Configured in task definition

**View logs:**

**Option A: CloudWatch Console**
1. Go to [CloudWatch Logs Console](https://console.aws.amazon.com/cloudwatch)
2. Click **"Logs"** → **"Log groups"**
3. Find log group: `/ecs/data-embed-ingestion-service` (or as configured)
4. Click on log stream to view

**Option B: ECS Console**
1. Go to [ECS Console](https://console.aws.amazon.com/ecs)
2. Select your cluster → service → task
3. Click **"Logs"** tab

**Option C: AWS CLI**
```bash
# Stream logs
aws logs tail /ecs/data-embed-ingestion-service --follow --region ap-south-1
```

---

### 6. **Kubernetes**

When deployed to Kubernetes:

**Logs appear in:**
- **kubectl logs** command
- **Pod stdout/stderr**

**View logs:**
```bash
# View logs for a pod
kubectl logs <pod-name> -n knowledge-base

# Follow logs
kubectl logs -f <pod-name> -n knowledge-base

# View logs for all pods in deployment
kubectl logs -f deployment/ingestion-service -n knowledge-base

# View last 100 lines
kubectl logs --tail=100 <pod-name> -n knowledge-base

# View logs with timestamps
kubectl logs --timestamps <pod-name> -n knowledge-base
```

---

## Log Levels

The application uses these log levels:

- **ERROR** - Errors that need attention (e.g., API failures, parsing errors)
- **WARN** - Warnings (e.g., unsupported file types, font parsing issues)
- **INFO** - General information (e.g., ingestion started, documents processed)
- **DEBUG** - Detailed debugging information (enabled for `com.nexa.ingestion` package)

## Log Format

Default format:
```
2026-02-11 14:30:25.123 [main] INFO  c.n.i.service.UnifiedIngestionService - Starting unified ingestion from all sources
```

Format: `TIMESTAMP [THREAD] LEVEL LOGGER - MESSAGE`

## Filtering Logs

### By Log Level
```bash
# Show only ERROR logs
docker logs ingestion-service 2>&1 | grep ERROR

# Show INFO and above
docker logs ingestion-service 2>&1 | grep -E "(INFO|WARN|ERROR)"
```

### By Component
```bash
# Show only ingestion logs
docker logs ingestion-service 2>&1 | grep "UnifiedIngestionService"

# Show only search logs
docker logs ingestion-service 2>&1 | grep "SearchController"
```

### By Time (if timestamps enabled)
```bash
# Show logs from last hour (if using file logging)
grep "2026-02-11 14:" logs/application.log
```

## Troubleshooting

### No logs appearing?

1. **Check log level** - Ensure `logging.level.root` is set appropriately
2. **Check file permissions** - If file logging enabled, ensure write permissions
3. **Check deployment** - Verify the application is actually running
4. **Check CloudWatch** - For AWS deployments, verify log group exists

### Logs too verbose?

Reduce log level in `application.yml`:
```yaml
logging:
  level:
    root: WARN  # Only show warnings and errors
    com.nexa.ingestion: INFO  # Less verbose for your app
```

### Need more detail?

Increase log level:
```yaml
logging:
  level:
    root: INFO
    com.nexa.ingestion: DEBUG  # More detailed logs
```
