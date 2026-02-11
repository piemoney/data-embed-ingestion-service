# AWS ECS Task Definition

This directory contains the ECS task definition for deploying the data-embed-ingestion-service to AWS ECS Fargate.

## Prerequisites

Before using this task definition, you need to:

1. **Create IAM Roles**:
   - **Task Execution Role** (`ecsTaskExecutionRole`): Allows ECS to pull images from ECR and fetch secrets from Secrets Manager
   - **Task Role** (`ecsTaskRole`): Grants permissions for the application to access AWS services (if needed)

2. **Create AWS Secrets Manager Secrets**:
   - `qdrant/host` - Qdrant host URL
   - `qdrant/api-key` - Qdrant API key
   - `huggingface/api-token` - Hugging Face API token
   - `confluence/username` - Confluence username (if using Confluence)
   - `confluence/api-token` - Confluence API token (if using Confluence)
   - `jira/username` - Jira username (if using Jira)
   - `jira/api-token` - Jira API token (if using Jira)
   - `github/api-token` - GitHub API token (if using GitHub)

3. **Create CloudWatch Log Group**:
   ```bash
   aws logs create-log-group --log-group-name /ecs/data-embed-ingestion-service --region ap-south-1
   ```

## Configuration

Before deploying, update the following placeholders in `task-definition.json`:

- `YOUR_ACCOUNT_ID` - Replace with your AWS account ID (appears in multiple places)
- `YOUR_ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com/nexa/common:latest` - Update with your actual ECR image URI (this will be replaced automatically by GitHub Actions)

## IAM Policies

### Task Execution Role Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:ap-south-1:YOUR_ACCOUNT_ID:secret:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:ap-south-1:YOUR_ACCOUNT_ID:log-group:/ecs/data-embed-ingestion-service:*"
    }
  ]
}
```

### Task Role Policy

Add policies here if your application needs to access other AWS services (S3, SQS, etc.).

## Deployment

The GitHub Actions workflow (`.github/workflows/aws.yml`) will automatically:
1. Build and push the Docker image to ECR
2. Update the task definition with the new image
3. Deploy the updated task definition to your ECS service

## Manual Deployment

If you need to deploy manually:

```bash
# Register the task definition
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definition.json \
  --region ap-south-1

# Update the service
aws ecs update-service \
  --cluster YOUR_CLUSTER_NAME \
  --service YOUR_SERVICE_NAME \
  --task-definition data-embed-ingestion-service \
  --region ap-south-1
```

## Resource Allocation

- **CPU**: 1024 (1 vCPU)
- **Memory**: 2048 MB (2 GB)

Adjust these values based on your workload. For production with high traffic, consider:
- CPU: 2048 (2 vCPU)
- Memory: 4096 MB (4 GB)

## Health Checks

The task definition includes a health check that queries `/actuator/health`. Ensure Spring Boot Actuator is enabled in your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Logging

Logs are automatically sent to CloudWatch Logs:
- Log Group: `/ecs/data-embed-ingestion-service`
- Log Stream: `ecs/{container-name}/{task-id}`

View logs:
```bash
aws logs tail /ecs/data-embed-ingestion-service --follow --region ap-south-1
```
