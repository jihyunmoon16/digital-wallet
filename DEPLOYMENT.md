# Deployment

## Architecture

```
Internet
    ↓
EC2 t3.micro (ap-southeast-2)
    ├── Spring Boot App (port 8080)
    └── Docker Redis (port 6379)
         ↓
RDS db.t3.micro (ap-southeast-2)
    └── PostgreSQL 17.6
```

## Infrastructure

| Component | Service | Spec |
|-----------|---------|------|
| Application server | AWS EC2 | t3.micro, Amazon Linux 2023 |
| Database | AWS RDS | db.t3.micro, PostgreSQL 17.6 |
| Cache | Docker on EC2 | Redis 7 |

## Prerequisites

- Java 21
- Docker
- AWS CLI

## Setup

### 1. EC2

```bash
# Java 21
sudo dnf install java-21-amazon-corretto -y

# Docker
sudo dnf install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
```

### 2. Redis

```bash
docker run -d --name redis -p 6379:6379 redis:7
```

### 3. Environment variables

```bash
export DB_PASSWORD=<rds-master-password>
```

### 4. Deploy

```bash
# Build locally
./gradlew clean build -x test

# Copy to EC2
scp -i <key>.pem build/libs/digital-wallet-0.0.1-SNAPSHOT.jar ec2-user@<ec2-ip>:~/

# Run on EC2
nohup java -jar \
  -Dspring.profiles.active=prod \
  ~/digital-wallet-0.0.1-SNAPSHOT.jar \
  > ~/app.log 2>&1 &
```

### 5. Verify

```bash
curl http://<ec2-ip>:8080/actuator/health
# {"status":"UP"}
```

## Logging

- Local: plain text with `requestId` pattern
- Production: JSON format (LogstashEncoder) to both console and file for CloudWatch ingestion
- Production log file path: `/home/ec2-user/logs/digital-wallet.json.log`

```json
{
  "@timestamp": "2026-03-19T05:39:34.924Z",
  "level": "INFO",
  "logger_name": "com.moon.digitalwallet.transfer.service.TransferService",
  "message": "Transfer from 1 to account 2",
  "requestId": "req-test-123"
}
```

## CloudWatch Monitoring

### 1. IAM role

- Attach `CloudWatchAgentServerPolicy` to the EC2 instance role.

### 2. Install CloudWatch Agent

```bash
sudo yum install amazon-cloudwatch-agent -y
sudo mkdir -p /home/ec2-user/logs
```

### 3. Copy agent config

- Repository file: `ops/cloudwatch/amazon-cloudwatch-agent.json`

```bash
sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
sudo cp ops/cloudwatch/amazon-cloudwatch-agent.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

### 4. Start agent

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s
```

### 5. Minimal alarms

- Application log metric filter:
  - log group: `/digital-wallet/application`
  - filter pattern example: `"level":"ERROR"`
  - metric name: `ApplicationErrorCount`
  - namespace: `DigitalWallet`

- Health/readiness alarm:
  - metric source: log metric filter or synthetic health check
  - alarm when consecutive failures are detected

- Instance health alarms:
  - `mem_used_percent`
  - `disk_used_percent`

### 6. Recommended dashboard widgets

- EC2 CPU utilization
- Memory used percent
- Disk used percent
- Application error count
- `/actuator/health/readiness` status check result

## Security groups

| Rule | Port | Source |
|------|------|--------|
| EC2 inbound - SSH | 22 | My IP |
| EC2 inbound - App | 8080 | 0.0.0.0/0 |
| RDS inbound - PostgreSQL | 5432 | EC2 security group |
