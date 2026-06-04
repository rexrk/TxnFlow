# Gateway Service

A Spring Cloud Gateway microservice that acts as the single entry point for all TxnFlow services, providing JWT authentication, request routing, and cross-cutting concerns like logging and rate limiting.

## Overview

The Gateway Service is the API gateway layer that:
- Routes incoming requests to backend microservices
- Validates JWT tokens (OAuth2 Resource Server)
- Applies request/response filters for logging
- Enforces rate limiting
- Centralizes cross-cutting concerns
- Provides health checks and observability

**Port:** 8081  
**Framework:** Spring Cloud Gateway

## Architecture

### Key Packages
- **`logging`** - Request/response logging filters
- **`security`** - OAuth2 configuration, JWT validation
- **`telemetry`** - OpenTelemetry instrumentation
- **`GatewayServiceApplication`** - Spring Boot entry point

### Technology Stack
- **Framework:** Spring Boot 4.0.6
- **Gateway:** Spring Cloud Gateway Server WebMVC 2025.1.1
- **Security:** Spring Security + OAuth2 Resource Server
- **Language:** Java 21
- **Observability:** OpenTelemetry
- **No Database** (stateless)

## Service Routes

All routes require JWT authentication via Bearer token.

### Route Configuration

| Service | Backend URL | Path Pattern | Port |
|---------|------------|--------------|------|
| Auth Service | http://localhost:8082 | `/api/v1/auth/**` | 8082 |
| Wallet Service | http://localhost:8083 | `/api/v1/wallets/**` | 8083 |
| Payment Service | http://localhost:8084 | `/api/v1/payments/**` | 8084 |
| Notification Service | http://localhost:8085 | `/api/v1/notifications/**` | 8085 |

### Route Details

#### 1. Auth Service Routes
```yaml
GET    /api/v1/auth/health              - Health check
POST   /api/v1/auth/register            - User registration (public, no auth)
POST   /api/v1/auth/login               - User login (public, no auth)
POST   /api/v1/auth/refresh-token       - Token refresh (requires token)
GET    /api/v1/auth/me                  - Current user info
```

#### 2. Wallet Service Routes
```yaml
GET    /api/v1/wallets/{walletId}       - Get wallet (requires auth)
GET    /api/v1/wallets/my/balance       - Get balance (requires auth)
POST   /api/v1/wallets/transfers        - Create transfer (requires auth)
GET    /api/v1/wallets/transactions     - Get transactions (requires auth)
```

#### 3. Payment Service Routes
```yaml
POST   /api/v1/payments/topup           - Create topup order (requires auth)
GET    /api/v1/payments/{ledgerId}      - Get payment status (requires auth)
POST   /api/v1/payments/razorpay/webhook - Razorpay webhook (no auth, IP-whitelisted)
```

#### 4. Notification Service Routes
```yaml
GET    /api/v1/notifications/health     - Health check
GET    /api/v1/notifications/logs       - Get notification logs (admin only)
```

## Security

### OAuth2 JWT Validation

All requests (except public endpoints) must include a JWT token in the Authorization header:

```http
Authorization: Bearer {jwt-token}
```

**Configuration:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KC_BASE_URL}/realms/txnflow
```

**Flow:**
1. Client requests `/api/v1/wallets/my/balance`
2. Gateway extracts JWT from Authorization header
3. Gateway validates JWT signature via Keycloak's public key
4. If valid, request proceeds with JWT claims in context
5. If invalid, returns 401 Unauthorized

### Public Endpoints (No Auth Required)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/payments/razorpay/webhook` (IP whitelist only)

### Token Format

JWT tokens from Keycloak contain:
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "iat": 1717500000,
  "exp": 1717503600,
  "iss": "http://localhost:8080/realms/txnflow"
}
```

## Request & Response Processing

### Request Logging Filter
Logs all incoming requests with:
- Method and path
- Client IP address
- JWT subject (user ID)
- Query parameters
- Request timestamp

```
[REQUEST] GET /api/v1/wallets/my/balance from 127.0.0.1 (user: a1b2c3d4-...)
```

### Response Logging Filter
Logs all outgoing responses with:
- HTTP status code
- Response time (ms)
- Response size (bytes)

```
[RESPONSE] 200 OK in 45ms (2.3KB)
```

## Configuration

### Environment Variables
```bash
# Keycloak
KC_BASE_URL=http://localhost:8080

# Backend Services (override as needed)
WALLET_SERVICE_URL=http://localhost:8083
PAYMENT_SERVICE_URL=http://localhost:8084
AUTH_SERVICE_URL=http://localhost:8082
NOTIFICATION_SERVICE_URL=http://localhost:8085

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

### Application Properties
```yaml
server:
  port: 8081
  servlet:
    context-path: /

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 10000
      filter:
        request-rate-limiter:
          key-resolver: principal_key_resolver
          redis-rate-limiter:
            replenish-rate: 100
            burst-capacity: 100
```

### application.yml Routes Configuration

See Spring Cloud Gateway documentation for detailed route configuration. Routes are typically defined as:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=2

        - id: wallet-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/wallets/**
          filters:
            - StripPrefix=2

        - id: payment-service
          uri: http://localhost:8084
          predicates:
            - Path=/api/v1/payments/**
          filters:
            - StripPrefix=2
```

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- Keycloak running on http://localhost:8080
- Backend services running (Auth, Wallet, Payment, Notification)

### Build & Run
```bash
cd gateway-service

# Build
mvn clean package

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Verify Gateway is Running
```bash
curl http://localhost:8081/actuator/health
```

## API Usage

### Example: Get Wallet Balance

1. **Obtain JWT token (from Auth Service)**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "..."
}
```

2. **Use token to call Wallet Service (via Gateway)**
```bash
curl -X GET http://localhost:8081/api/v1/wallets/my/balance \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

Response:
{
  "balance": "5000.00",
  "currency": "INR",
  "walletId": "uuid-xxx"
}
```

## Logging

### Enable Debug Logging
```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    txnflow: DEBUG
```

### Log Files
Logs are output to console by default. To file:
```yaml
logging:
  file:
    name: logs/gateway-service.log
    max-size: 10MB
    max-history: 10
```

## Performance & Scaling

### Connection Pooling
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 1000
          pending-acquire-max-count: 100
          pending-acquire-timeout: 30s
```

### Request Timeouts
- Connect timeout: 5s
- Response timeout: 10s

### Concurrency
Gateway uses virtual threads (Project Loom) for efficient concurrency:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

## Observability

### Health Checks
```bash
curl http://localhost:8081/actuator/health

Response:
{
  "status": "UP",
  "components": {
    "authService": { "status": "UP" },
    "walletService": { "status": "UP" },
    "paymentService": { "status": "UP" }
  }
}
```

### Metrics
```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/http.server.requests
```

### Distributed Tracing
All requests are traced via OpenTelemetry:
```bash
curl http://localhost:8081/api/v1/wallets/balance \
  -H "Authorization: Bearer {token}"

# Trace visible in OpenTelemetry Jaeger UI (if configured)
```

## Troubleshooting

### 401 Unauthorized Error
1. Verify JWT token is valid: `jwt.io` (decode and check `exp`)
2. Verify Keycloak is running: `curl http://localhost:8080`
3. Check JWT issuer matches configuration: Should be `http://localhost:8080/realms/txnflow`

### 502 Bad Gateway
1. Verify backend service is running:
   ```bash
   curl http://localhost:8083/actuator/health  # Wallet Service
   ```
2. Check response timeout (10s default)
3. Review gateway logs for specific errors

### High Latency
1. Check backend service response times
2. Monitor database query performance
3. Review gateway request logging for bottlenecks

### JWT Validation Failing
1. Verify Keycloak certificate is trusted
2. Check clock skew between Gateway and Keycloak
3. Validate token has not expired: `exp` claim

## Related Services

- **Auth-Service** (8082) - Issues JWT tokens
- **Wallet-Service** (8083) - Core wallet operations
- **Payment-Adapter-Service** (8084) - Payment processing
- **Notification-Service** (8085) - Event notifications

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Security OAuth2 Resource Server](https://spring.io/projects/spring-security-oauth2-resource-server)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [TxnFlow Architecture](../README.md)

## Development Tips

### Local Testing with cURL

**1. Register User**
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@123"}'
```

**2. Login to Get Token**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@123"}' \
  | jq -r '.token')
```

**3. Use Token for Authenticated Requests**
```bash
curl -X GET http://localhost:8081/api/v1/wallets/my/balance \
  -H "Authorization: Bearer $TOKEN"
```

### Using Postman

1. Set up Postman environment variables:
   ```
   base_url: http://localhost:8081
   token: (get from login response)
   ```

2. Use `{{token}}` in Authorization header for authenticated requests

3. Pre-request script to auto-refresh token:
   ```javascript
   // Auto-refresh if expired
   const token = pm.environment.get('token');
   // ... implement refresh logic
   ```
