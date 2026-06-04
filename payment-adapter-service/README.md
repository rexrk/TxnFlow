# Payment Adapter Service

A Spring Boot microservice that integrates with Razorpay payment gateway, handling payment orders, webhook verification, and transactional outbox pattern for reliable event publishing.

## Overview

The Payment Adapter Service acts as an adapter between TxnFlow and Razorpay, providing:
- Payment order creation (topup functionality)
- HMAC-SHA256 webhook signature verification
- Payment status tracking and reconciliation
- Transactional outbox pattern for Kafka publishing
- Idempotent payment processing

**Port:** 8084  
**Context Path:** `/api/v1/payments`

## Architecture

### Key Packages
- **`service`** - `PaymentService`, `RazorpayWebhookService` (core logic)
- **`controller`** - `PaymentController`, `RazorpayController` (API endpoints)
- **`entity`** - `PaymentLedger`, `OutboxEvent` (persistence)
- **`dto`** - Request/Response DTOs, Event models
- **`repository`** - JPA repositories for data access
- **`scheduler`** - Scheduled tasks for payment reconciliation
- **`security`** - OAuth2 JWT configuration

### Technology Stack
- **Framework:** Spring Boot 4.0.6
- **Language:** Java 21
- **Database:** PostgreSQL (payment_db)
- **Payment Gateway:** Razorpay Java SDK v1.4.8
- **Async:** Apache Kafka (localhost:9092)
- **Authentication:** Keycloak (OAuth2 Resource Server)
- **Observability:** OpenTelemetry
- **Testing:** JUnit 5 + Mockito

## Database

### PostgreSQL Configuration
```yaml
Database: payment_db
User: txnflow
Password: txnflow_password
JDBC URL: jdbc:postgresql://localhost:5432/payment_db
```

### Schema

**PaymentLedger** - Tracks all payment orders
```sql
id (UUID, PK)
user_id (UUID, FK)
amount (BigDecimal)
currency (VARCHAR)
status (ENUM: PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED)
razorpay_order_id (VARCHAR, UNIQUE)
razorpay_payment_id (VARCHAR)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)
```

**OutboxEvent** - Transactional outbox for reliable event publishing
```sql
id (BIGINT, PK)
aggregation_id (UUID)
event_type (VARCHAR)
payload (JSON)
created_at (TIMESTAMP)
processed (BOOLEAN, INDEX)
```

### DDL
Schema auto-creation via JPA: `spring.jpa.hibernate.ddl-auto=update`

## Razorpay Integration

### Configuration
```yaml
razorpay:
  key-id: ${RAZORPAY_KEY_ID}           # e.g., rzp_test_1234567890
  key-secret: ${RAZORPAY_KEY_SECRET}   # e.g., aBcDeFgHiJkLmNoPqRsT
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}  # For signature verification
```

### Webhook Verification
All webhook payloads are verified using HMAC-SHA256:
```java
String signature = request.getHeader("X-Razorpay-Signature");
String generatedSignature = hmacSha256(webhook_body, webhook_secret);
assert(signature.equals(generatedSignature));
```

### Supported Events
- `payment.authorized` - Payment captured/authorized
- `payment.failed` - Payment failed
- `payment.captured` - Payment settled
- `refund.created` - Refund initiated

## Kafka Integration

### Events Produced
- **`TopupCompletedEvent`** → When payment is successfully captured
- **`TopupFailedEvent`** → When payment fails or is canceled

### Transactional Outbox Pattern
Instead of directly publishing to Kafka, events are:
1. Inserted to `OutboxEvent` table (same transaction as payment update)
2. Background job polls `OutboxEvent` for unprocessed events
3. Published to Kafka and marked as processed

This ensures **no lost events** even if the service crashes.

### Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
```

## Security

**OAuth2 Resource Server:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KC_BASE_URL}/realms/txnflow
```

## API Endpoints

### Create Topup Payment
```http
POST /api/v1/payments/topup
Content-Type: application/json
Authorization: Bearer {jwt-token}

{
  "amount": 10000,
  "currency": "INR"
}

Response:
{
  "ledgerId": "uuid",
  "razorpayOrderId": "order_1234567890",
  "amount": 10000,
  "currency": "INR",
  "status": "PENDING",
  "checkoutUrl": "http://localhost:8081/checkout/{ledgerId}",
  "createdAt": "2026-06-04T14:00:00Z"
}
```

### Get Payment Details
```http
GET /api/v1/payments/{ledgerId}
Authorization: Bearer {jwt-token}

Response:
{
  "ledgerId": "uuid",
  "amount": 10000,
  "status": "CAPTURED",
  "razorpayPaymentId": "pay_1234567890",
  "createdAt": "2026-06-04T14:00:00Z",
  "updatedAt": "2026-06-04T14:05:00Z"
}
```

### Razorpay Webhook Handler (Internal)
```http
POST /api/v1/payments/razorpay/webhook
X-Razorpay-Signature: {signature}
Content-Type: application/json

{
  "event": "payment.authorized",
  "payload": { ... }
}

Response: 204 No Content
```

## Configuration

### Environment Variables
```bash
# Razorpay Credentials (https://dashboard.razorpay.com/app/keys)
RAZORPAY_KEY_ID=rzp_test_xxxxxx
RAZORPAY_KEY_SECRET=xxxxxx
RAZORPAY_WEBHOOK_SECRET=webhook_secret_xxxxx

# Keycloak
KC_BASE_URL=http://localhost:8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/payment_db
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
  port: 8084
  servlet:
    context-path: /api/v1/payments

spring:
  application:
    name: payment-adapter-service
  jpa:
    hibernate:
      ddl-auto: update
  scheduling:
    enabled: true
```

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL running with payment_db created
- Kafka running on localhost:9092
- Keycloak running on http://localhost:8080
- Razorpay test account (sign up at https://razorpay.com)

### Build & Run
```bash
cd payment-adapter-service

# Build
mvn clean package

# Export Razorpay credentials
export RAZORPAY_KEY_ID=rzp_test_xxxxx
export RAZORPAY_KEY_SECRET=xxxxx
export RAZORPAY_WEBHOOK_SECRET=xxxxx

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Initialize Database
```bash
psql -U postgres
CREATE DATABASE payment_db;
\c payment_db
-- Tables auto-created by Hibernate
```

### Testing Webhook Locally
Use ngrok to expose local service:
```bash
ngrok http 8084
# Copy forwarding URL and update in Razorpay Dashboard:
# Settings > Webhooks > Add > https://your-ngrok-url/api/v1/payments/razorpay/webhook
```

## Testing

Run all tests:
```bash
mvn test
```

### Test Classes
- Core payment logic tests
- Webhook signature verification tests
- Outbox event publishing tests

## Scheduled Tasks

### Outbox Event Publisher
Polls `OutboxEvent` table every 5 seconds for unprocessed events:
```yaml
scheduling:
  fixed-rate: 5000  # 5 seconds
  max-attempts: 3
```

If Kafka publish fails, event is retried up to 3 times.

## Key Concepts

### Transactional Outbox
Ensures no events are lost when publishing to Kafka:
```java
@Transactional
void completePayment(PaymentLedger payment) {
  paymentLedger.setStatus(CAPTURED);
  paymentRepository.save(payment);  // Same transaction
  
  OutboxEvent event = new OutboxEvent(TopupCompletedEvent);
  outboxEventRepository.save(event);  // Same transaction
}
```

### Idempotency
Payment orders are idempotent. Creating the same order twice returns the same result.

### Webhook Signature Verification
All Razorpay webhooks are verified to prevent forged requests:
```java
String signature = calculateHmacSha256(body, webhook_secret);
if (!signature.equals(headerSignature)) {
  throw new WebhookVerificationException();
}
```

## Troubleshooting

### Razorpay API Errors
1. Verify API key and secret in Razorpay Dashboard
2. Ensure webhook secret matches in settings
3. Check rate limits (100 requests/second)

### Webhook Not Received
1. Verify webhook URL is publicly accessible
2. Check Razorpay webhook logs in Dashboard
3. Ensure port 8084 is not blocked by firewall

### OutboxEvent Not Published
1. Check Kafka connectivity: `nc -zv localhost 9092`
2. Review application logs for Kafka errors
3. Verify `spring.scheduling.enabled=true`

## Related Services

- **Wallet-Service** (8083) - Credits wallet on payment success
- **Notification-Service** (8085) - Sends payment confirmation emails
- **Gateway-Service** (8081) - Routes payment requests
- **Auth-Service** (8082) - User authentication

## References

- [Razorpay API Documentation](https://razorpay.com/docs/)
- [Razorpay Java SDK](https://github.com/razorpay/razorpay-java)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [TxnFlow Architecture](../README.md)
