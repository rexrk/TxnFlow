# Auth Service

A Spring Boot microservice that handles user authentication, JWT token generation, and role-based access control (RBAC) for the TxnFlow platform.

## Overview

The Auth Service provides:
- User registration with email and password
- User login with JWT token generation
- Refresh token flow for token renewal
- BCrypt password hashing for security
- Basic RBAC (USER / ADMIN roles)
- OAuth2 token endpoint compatibility

**Port:** 8082  
**Context Path:** `/api/v1/auth`

## Architecture

### Key Packages
- **`user`** - `User` entity, `UserRepository`, user management
- **`auth`** - `AuthController`, `AuthService`, login/register logic
- **`security`** - JWT token generation, BCrypt configuration
- **`token`** - `RefreshToken` entity and management
- **`dto`** - Request/Response DTOs
- **`exception`** - Custom exceptions and handlers

### Technology Stack
- **Framework:** Spring Boot 4.0.6
- **Language:** Java 21
- **Database:** PostgreSQL (auth_db)
- **Security:** Spring Security + JWT
- **Password Hashing:** BCrypt
- **Observability:** OpenTelemetry
- **Testing:** JUnit 5 + Mockito

## Database

### PostgreSQL Configuration
```yaml
Database: auth_db
User: txnflow
Password: txnflow_password
JDBC URL: jdbc:postgresql://localhost:5432/auth_db
```

### Schema

**User** - User accounts
```sql
id (UUID, PK)
email (VARCHAR, UNIQUE)
password_hash (VARCHAR)  -- BCrypt hash
first_name (VARCHAR)
last_name (VARCHAR)
roles (VARCHAR)  -- ENUM: USER, ADMIN
enabled (BOOLEAN)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)
last_login_at (TIMESTAMP)
```

**RefreshToken** - Token refresh tracking
```sql
id (UUID, PK)
user_id (UUID, FK)
token_value (VARCHAR, UNIQUE)
expires_at (TIMESTAMP)
revoked (BOOLEAN)
created_at (TIMESTAMP)
```

### DDL
Schema auto-creation via JPA: `spring.jpa.hibernate.ddl-auto=update`

## JWT Token Management

### Token Structure

**Access Token** (JWT)
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "roles": ["USER"],
  "iat": 1717500000,
  "exp": 1717503600,
  "iss": "http://localhost:8080/realms/txnflow"
}
```

**Token Lifetime**
- Access Token: 1 hour
- Refresh Token: 7 days

### Token Generation
Uses HMAC-SHA256 signing with a secret key configured in environment.

## API Endpoints

### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
{
  "userId": "uuid-xxx",
  "email": "user@example.com",
  "message": "User registered successfully"
}
```

### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### Refresh Token
```http
POST /api/v1/auth/refresh-token
Content-Type: application/json
Authorization: Bearer {refresh-token}

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

### Get Current User
```http
GET /api/v1/auth/me
Authorization: Bearer {access-token}

Response: 200 OK
{
  "userId": "uuid-xxx",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"]
}
```

### Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer {access-token}

Response: 200 OK
{
  "message": "Logged out successfully"
}
```

## Configuration

### Environment Variables
```bash
# JWT
JWT_SECRET=your-256-bit-secret-key-here-min-32-chars
JWT_EXPIRATION_MS=3600000  # 1 hour in milliseconds
REFRESH_TOKEN_EXPIRATION_MS=604800000  # 7 days

# Keycloak
KC_BASE_URL=http://localhost:8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
SPRING_DATASOURCE_USERNAME=txnflow
SPRING_DATASOURCE_PASSWORD=txnflow_password

# Observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

### Application Properties
```yaml
server:
  port: 8082
  servlet:
    context-path: /api/v1/auth

spring:
  application:
    name: auth-service
  jpa:
    hibernate:
      ddl-auto: update
  security:
    user:
      name: admin
      password: admin

auth:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION_MS}
    refresh-expiration: ${REFRESH_TOKEN_EXPIRATION_MS}
```

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL running with auth_db created
- Keycloak running on http://localhost:8080 (optional for Keycloak integration)

### Build & Run
```bash
cd auth-service

# Build
mvn clean package

# Set environment variables
export JWT_SECRET="your-256-bit-secret-key-here-minimum-32-characters"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/auth_db"
export SPRING_DATASOURCE_USERNAME="txnflow"
export SPRING_DATASOURCE_PASSWORD="txnflow_password"

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Initialize Database
```bash
psql -U postgres
CREATE DATABASE auth_db;
\c auth_db
-- Tables auto-created by Hibernate
```

### Verify Service is Running
```bash
curl http://localhost:8082/api/v1/auth/health
```

## Testing

Run all tests:
```bash
mvn test
```

### Test Classes
- User registration tests
- Login and JWT generation tests
- Token refresh tests
- Password hashing tests
- RBAC tests

## Security Considerations

### Password Security
- All passwords hashed using BCrypt (strength 12)
- Never stored in plaintext
- Passwords validated on each login

```java
PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
String hashed = passwordEncoder.encode(plainPassword);
```

### JWT Security
- Signed with HMAC-SHA256
- Includes expiration time (`exp` claim)
- Should only be transmitted over HTTPS in production

### Token Revocation
Refresh tokens can be revoked on logout:
```sql
UPDATE refresh_token SET revoked = TRUE WHERE id = '...';
```

## Running with Docker

### Build Image
```bash
mvn clean package
docker build -t txnflow/auth-service:latest .
```

### Run Container
```bash
docker run -d \
  --name auth-service \
  -p 8082:8082 \
  -e JWT_SECRET="your-secret" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/auth_db" \
  -e SPRING_DATASOURCE_USERNAME="txnflow" \
  -e SPRING_DATASOURCE_PASSWORD="txnflow_password" \
  txnflow/auth-service:latest
```

## Troubleshooting

### Database Timezone Issue
Add JVM option to set timezone:
```bash
-Duser.timezone=Asia/Kolkata
```

Or set environment variable:
```bash
export TZ=Asia/Kolkata
```

### JWT Secret Not Set
```
Error: JWT_SECRET environment variable not set
```

Solution:
```bash
export JWT_SECRET="generate-32-character-secret-key"
```

### Database Connection Failed
```bash
# Test PostgreSQL connection
psql -h localhost -U txnflow -d auth_db

# Or check connection string
echo $SPRING_DATASOURCE_URL
```

### Token Validation Error
1. Verify JWT_SECRET matches across services
2. Check token expiration: `exp` claim > current timestamp
3. Validate token signature: `jwt.io`

## Best Practices

### Client-Side
1. Store JWT in memory or secure cookie (not localStorage for security)
2. Include JWT in all API requests: `Authorization: Bearer {token}`
3. Refresh token before expiration
4. Clear token on logout

### Server-Side
1. Validate token signature on every request
2. Check token expiration
3. Revoke refresh tokens on logout
4. Use HTTPS in production
5. Rotate JWT secret periodically

## Related Services

- **Gateway-Service** (8081) - Routes auth requests
- **Wallet-Service** (8083) - Validates JWT from auth service
- **Payment-Adapter-Service** (8084) - Validates JWT

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io - JWT Introduction](https://jwt.io/introduction)
- [BCrypt Password Hashing](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [TxnFlow Architecture](../README.md)

## API Client Example

### JavaScript/TypeScript
```typescript
// Register
const registerResponse = await fetch('http://localhost:8082/api/v1/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!',
    firstName: 'John',
    lastName: 'Doe'
  })
});

// Login
const loginResponse = await fetch('http://localhost:8082/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!'
  })
});

const { accessToken } = await loginResponse.json();

// Use token
const walletResponse = await fetch('http://localhost:8081/api/v1/wallets/my/balance', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

### cURL
```bash
# Register
curl -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!","firstName":"John","lastName":"Doe"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePassword123!"}' \
  | jq -r '.accessToken')

# Use token
curl -X GET http://localhost:8081/api/v1/wallets/my/balance \
  -H "Authorization: Bearer $TOKEN"
```