# Notification Service

A Spring Boot microservice that asynchronously consumes Kafka events and sends email notifications via Resend API, enabling real-time user communication for transactions and account events.

## Overview

The Notification Service is an event-driven microservice that handles:
- Kafka event consumption (asynchronous processing)
- Email notification sending via Resend API
- Event handling for user registration, topups, transfers, and failures
- Notification audit trail

**Port:** 8085  
**Context Path:** `/api/v1/notifications`

## Architecture

### Key Packages
- **`listener`** - `NotificationListener` (Kafka @KafkaListener)
- **`service`** - `EmailService`, `NotificationService` (business logic)
- **`entity`** - `NotificationEvent` (audit log)
- **`dto`** - Event models: `UserRegisteredEvent`, `TopupCompletedEvent`, `TopupFailedEvent`, `TransferCompletedEvent`
- **`template`** - Email templates (HTML)
- **`properties`** - `ResendProperties` (config)
- **`controller`** - REST endpoints (health, status)

### Technology Stack
- **Framework:** Spring Boot 4.0.6
- **Language:** Java 21
- **Database:** PostgreSQL (notification_db)
- **Message Queue:** Apache Kafka (localhost:9092)
- **Email Provider:** Resend API
- **Testing:** JUnit 5 + Mockito

## Database

### PostgreSQL Configuration
```yaml
Database: notification_db
User: txnflow
Password: txnflow_password
JDBC URL: jdbc:postgresql://localhost:5432/notification_db
```

### Schema

**NotificationEvent** - Audit log of sent notifications
```sql
id (BIGINT, PK)
event_type (VARCHAR)  -- ENUM: USER_REGISTERED, TOPUP_COMPLETED, TRANSFER_COMPLETED, etc.
user_id (UUID)
recipient_email (VARCHAR)
subject (VARCHAR)
status (VARCHAR)  -- SENT, FAILED, BOUNCED
error_message (TEXT)
kafka_partition (INT)
kafka_offset (BIGINT)
created_at (TIMESTAMP)
sent_at (TIMESTAMP)
```

### DDL
Schema auto-creation via JPA: `spring.jpa.hibernate.ddl-auto=update`

## Kafka Integration

### Events Consumed

**1. UserRegisteredEvent**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "timestamp": "2026-06-04T14:00:00Z"
}
```
→ Sends welcome email

**2. TopupCompletedEvent**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "amount": 10000,
  "currency": "INR",
  "orderId": "order_xxx",
  "timestamp": "2026-06-04T14:00:00Z"
}
```
→ Sends topup confirmation email

**3. TopupFailedEvent**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "amount": 10000,
  "currency": "INR",
  "reason": "Payment declined",
  "timestamp": "2026-06-04T14:00:00Z"
}
```
→ Sends topup failure notification

**4. TransferCompletedEvent**
```json
{
  "senderId": "uuid",
  "senderEmail": "sender@example.com",
  "receiverId": "uuid",
  "receiverEmail": "receiver@example.com",
  "amount": 5000,
  "currency": "INR",
  "transferId": "transfer_xxx",
  "timestamp": "2026-06-04T14:00:00Z"
}
```
→ Sends transfer confirmation to both parties

### Kafka Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      enable-auto-commit: true
      max-poll-records: 50
    listener:
      concurrency: 3
      max-concurrency: 10
```

**Consumer Group:** `notification-service`

## Resend Email Integration

### Configuration
```yaml
resend:
  api-key: ${RESEND_API_KEY}
  from-email: event@mail.txnflow.dpdns.org
  from-name: TxnFlow
  api-url: https://api.resend.com
```

### Supported Email Events

| Event | Template | Recipient | Purpose |
|-------|----------|-----------|---------|
| USER_REGISTERED | WelcomeEmail | New user | Welcome & onboarding |
| TOPUP_COMPLETED | TopupConfirmation | User | Payment confirmation |
| TOPUP_FAILED | TopupFailure | User | Payment failure notification |
| TRANSFER_COMPLETED | TransferConfirmation | Sender + Receiver | Transfer receipt |

## API Endpoints

### Health Check
```http
GET /api/v1/notifications/health
Response: 200 OK
{
  "status": "UP",
  "kafka": "CONNECTED",
  "database": "CONNECTED"
}
```

### Get Notification Status (Admin)
```http
GET /api/v1/notifications/status/{userId}
Authorization: Bearer {jwt-token}

Response: 200 OK
{
  "userId": "uuid",
  "lastNotificationAt": "2026-06-04T14:00:00Z",
  "totalSent": 12,
  "totalFailed": 1
}
```

### List Recent Notifications (Admin)
```http
GET /api/v1/notifications/logs?limit=50&offset=0
Authorization: Bearer {jwt-token}

Response: 200 OK
[
  {
    "id": 1,
    "eventType": "USER_REGISTERED",
    "userId": "uuid",
    "email": "user@example.com",
    "status": "SENT",
    "createdAt": "2026-06-04T14:00:00Z",
    "sentAt": "2026-06-04T14:00:05Z"
  }
]
```

## Configuration

### Environment Variables
```bash
# Resend API
RESEND_API_KEY=re_xxxxxxxxxxxx

# Keycloak
KC_BASE_URL=http://localhost:8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notification_db
SPRING_DATASOURCE_USERNAME=txnflow
SPRING_DATASOURCE_PASSWORD=txnflow_password

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

### Application Properties
```yaml
server:
  port: 8085
  servlet:
    context-path: /api/v1/notifications

spring:
  application:
    name: notification-service
  jpa:
    hibernate:
      ddl-auto: update
  kafka:
    listener:
      concurrency: 3

logging:
  level:
    root: INFO
    txnflow.walletservice: DEBUG
```

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL running with notification_db created
- Kafka running on localhost:9092
- Keycloak running on http://localhost:8080
- Resend API key (sign up at https://resend.com)

### Build & Run
```bash
cd notification-service

# Build
mvn clean package

# Export Resend API key
export RESEND_API_KEY=re_xxxxxxxxxxxxxxxx

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Initialize Database
```bash
psql -U postgres
CREATE DATABASE notification_db;
\c notification_db
-- Tables auto-created by Hibernate
```

### Verify Kafka Connection
```bash
# Check Kafka is running
nc -zv localhost 9092

# List topics
kafka-topics --bootstrap-server localhost:9092 --list
```

## Testing

Run all tests:
```bash
mvn test
```

### Test Classes
- Kafka listener tests (with mocked Kafka)
- Email template rendering tests
- Resend API integration tests

## Monitoring

### Kafka Consumer Lag
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-service --describe
```

### Application Metrics
```http
GET http://localhost:8085/actuator/metrics
```

### Log Monitoring
Watch for these log patterns:
```
ERROR - Failed to send notification to user
WARN  - Kafka consumer lag detected
INFO  - Successfully sent {eventType} to {email}
```

## Error Handling

### Retry Strategy
- **Failed Email:** Retried up to 3 times with exponential backoff
- **Kafka Consumer Error:** Event logged; processing continues (no poison pill)
- **Database Error:** Transaction rolled back; event remains unprocessed

### Dead Letter Handling
Events that fail 3 times are logged with details for manual investigation:
```
NotificationEvent.status = FAILED
NotificationEvent.error_message = "Resend API: rate limit exceeded"
```

## Email Templates

### Welcome Email (USER_REGISTERED)
```html
Subject: Welcome to TxnFlow!
Body: 
  - Welcome greeting
  - Account setup instructions
  - Link to verify email
  - Dashboard link
```

### Topup Confirmation (TOPUP_COMPLETED)
```html
Subject: ₹{amount} Topup Successful
Body:
  - Amount and date
  - Order ID
  - Transaction receipt
  - Balance update
```

### Transfer Confirmation (TRANSFER_COMPLETED)
**For Sender:**
```html
Subject: Transfer Sent - ₹{amount} to {receiverName}
Body:
  - Transfer details (sender → receiver)
  - Amount and date
  - New balance
  - Reference number
```

**For Receiver:**
```html
Subject: Transfer Received - ₹{amount} from {senderName}
Body:
  - Transfer details
  - Amount and date
  - New balance
  - Add sender to contacts option
```

## Troubleshooting

### Kafka Consumer Not Processing Events
1. Verify Kafka is running: `nc -zv localhost 9092`
2. Check consumer group offset: `kafka-consumer-groups ... --describe`
3. Review application logs for errors
4. Restart service if stuck: `mvn spring-boot:run`

### Emails Not Sending
1. Verify RESEND_API_KEY is set: `echo $RESEND_API_KEY`
2. Check Resend dashboard for API errors
3. Verify sender email domain is verified in Resend
4. Check NotificationEvent table for error_message

### High Kafka Lag
1. Increase consumer concurrency:
   ```yaml
   spring.kafka.listener.concurrency: 5
   ```
2. Check Resend API rate limits (100 req/sec)
3. Monitor database performance

### Database Connection Issues
```bash
psql -U txnflow -d notification_db -h localhost
```

## Related Services

- **Wallet-Service** (8083) - Publishes TopupCompleted, TransferCompleted events
- **Auth-Service** (8082) - Publishes UserRegistered events
- **Payment-Adapter-Service** (8084) - Publishes TopupFailed events
- **Gateway-Service** (8081) - Routes notification API calls

## References

- [Resend API Documentation](https://resend.com/docs)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Guide](https://spring.io/projects/spring-kafka)
- [TxnFlow Architecture](../README.md)

## Debugging Kafka Events

### View Event Topics
```bash
# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# Describe a topic
kafka-topics --bootstrap-server localhost:9092 --describe --topic topupCompletedEvent
```

### Consume Events Manually
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic topupCompletedEvent \
  --from-beginning \
  --property print.key=true
```

### Check Consumer Group Status
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-service \
  --describe

# Reset offset (if needed)
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-service \
  --reset-offsets --to-earliest --execute
```
