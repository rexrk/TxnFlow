# Wallet Service

A Spring Boot microservice that manages digital wallets, balances, and inter-wallet transfers with strong consistency guarantees and idempotency support.

## Overview

The Wallet Service is the core business logic layer of TxnFlow, handling:
- Wallet creation and management
- Real-time balance tracking
- Safe peer-to-peer transfers (with pessimistic locking)
- Immutable transaction history
- Idempotency key management to prevent duplicate transactions
- Kafka event orchestration

**Port:** 8083  
**Context Path:** `/api/v1/wallets`

## Architecture

### Key Packages
- **`wallet`** - Wallet entity, repository, and core service
- **`transaction`** - Transaction history, ledger, and queries
- **`transfer`** - Transfer orchestration and validation
- **`orchestration`** - TopupProcessor, TransferProcessor (business logic)
- **`kafka`** - Event listeners and producers (Kafka integration)
- **`security`** - OAuth2 JWT authentication

### Technology Stack
- **Framework:** Spring Boot 4.0.6
- **Language:** Java 21
- **Database:** PostgreSQL (wallet_db)
- **Async:** Apache Kafka (localhost:9092)
- **Authentication:** Keycloak (OAuth2 Resource Server)
- **Observability:** OpenTelemetry
- **Testing:** JUnit 5 + Mockito

## Database

### PostgreSQL Configuration
```yaml
Database: wallet_db
User: txnflow
Password: txnflow_password
JDBC URL: jdbc:postgresql://localhost:5432/wallet_db
```

### Schema
- **Wallet** - User wallets with balance tracking
- **WalletTransaction** - Immutable transaction ledger
- **WalletTransfer** - Transfer records (orchestration)
- **IdempotencyRecord** - Tracks idempotency keys to prevent duplicates

### DDL
Schema auto-creation via JPA: `spring.jpa.hibernate.ddl-auto=update`

## Kafka Integration

### Events Produced
- **`TopupCompletedEvent`** → Published when a topup/payment is processed
- **`TransferCompletedEvent`** → Published when a transfer completes
- **`UserRegisteredEvent`** → Published when a new user registers (from Auth Service)

### Events Consumed
- **`TopupCompletedEvent`** - Triggered by Payment-Adapter-Service (triggers wallet credit)
- **`UserRegisteredEvent`** - Triggered by Auth-Service (new wallet creation)

### Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: wallet-service
      auto-offset-reset: earliest
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

Keycloak validates all JWT tokens. Services communicate via Bearer token in Authorization header.

## API Endpoints

### Wallet Management
```
POST   /api/v1/wallets/create           - Create new wallet
GET    /api/v1/wallets/{walletId}       - Get wallet details
GET    /api/v1/wallets/my/balance       - Get current balance
```

### Transfers
```
POST   /api/v1/wallets/transfers        - Initiate transfer (idempotent)
GET    /api/v1/wallets/transfers/{id}   - Get transfer status
```

### Transaction History
```
GET    /api/v1/wallets/transactions     - Get recent transactions (20 limit)
GET    /api/v1/wallets/transactions/{id} - Get transaction details
GET    /api/v1/wallets/transactions/range - Get transactions in date range
```

## Configuration

### Environment Variables
```bash
# Keycloak
KC_BASE_URL=http://localhost:8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet_db
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
  port: 8083
  servlet:
    context-path: /api/v1/wallets

spring:
  application:
    name: wallet-service
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL running with wallet_db created
- Kafka running on localhost:9092
- Keycloak running on http://localhost:8080

### Build & Run
```bash
cd wallet-service

# Build
mvn clean package

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
# OR
java -jar target/wallet-service-0.0.1-SNAPSHOT.jar
```

### Initialize Database
```bash
psql -U postgres
CREATE DATABASE wallet_db;
\c wallet_db
-- Tables auto-created by Hibernate
```

## Testing

Run all tests:
```bash
mvn test
```

**Note:** Kafka is mocked in tests via `TestKafkaConfig` (active in test profile). Kafka listeners are disabled via `application-test.yml`.

### Test Classes
- `TransferProcessorTest` - Core transfer logic (10 test cases)
- `DefaultTransferServiceTest` - Transfer service layer (7 test cases)
- `DefaultTransactionServiceTest` - Transaction queries (6 test cases)
- `WalletServiceApplicationTests` - Context loading test

## Key Concepts

### Idempotency
Transfers are idempotent using idempotency keys. Sending the same request twice returns the same result without side effects.

**Implementation:**
- Client provides unique `idempotencyKey` per request
- Service stores key + request hash
- Duplicate requests return cached response

### Pessimistic Locking
Concurrent transfers from the same wallet are serialized via row-level locks to prevent balance corruption.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Wallet wallet = walletRepository.findByIdForUpdate(walletId);
```

### Immutable Ledger
Transactions are immutable records. No updates or deletes—only inserts.

### Double-Entry Bookkeeping
Each transfer creates two balanced transactions:
- DEBIT on sender's wallet
- CREDIT on receiver's wallet

## Troubleshooting

### Database Timezone Issue
Add JVM option:
```bash
-Duser.timezone=Asia/Kolkata
```

### Kafka Connection Failed
Ensure Kafka is running:
```bash
# Start Kafka
docker run -d -p 9092:9092 confluentinc/cp-kafka
```

### JWT Validation Error
Verify Keycloak is running and `KC_BASE_URL` is correct:
```bash
curl http://localhost:8080/realms/txnflow/.well-known/openid-configuration
```

## Related Services

- **Auth-Service** (8082) - User registration & JWT token generation
- **Payment-Adapter-Service** (8084) - Razorpay topup integration
- **Notification-Service** (8085) - Email notifications for transfers
- **Gateway-Service** (8081) - API gateway & JWT validation

## References

- [TxnFlow Architecture](../README.md)
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Kafka: https://spring.io/projects/spring-kafka
- PostgreSQL Documentation: https://www.postgresql.org/docs/
