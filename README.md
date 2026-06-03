```md

TxnFlow — Payment Reliability Backend System

External Systems:
Cloudflare tunnel 
```
cloudflared tunnel run rexrk-tunnel
```

Goal:
Build a backend-focused digital wallet/payment system demonstrating reliable transaction handling, idempotency, ledger architecture, webhook verification, async event processing, and Kubernetes deployment.

---

## PROJECT STRUCTURE

Txn-Flow/
├── auth-service/                   # 1. JWT auth, users, roles
├── wallet-service/            # 2. wallet, balance, transfer, ledger, idempotency
├── payment-adapter-service/        # 3. Razorpay integration + webhook verification
├── notification-service/           # 4. async transaction notifications
├── gateway-service/                # 5. routing, auth filter, rate limiting
├── config-service/                 # 6. centralized config
├── discovery-service/              # 7. service registry
├── k8s/                            # 8. Kubernetes manifests
├── docs/                           # 9. architecture, flows, failure cases
├── README.md                       # 10. docs
└── .gitignore

---

## Completion timeline
- [x] auth-service
- [x] gateway-service
- [x] wallet-core-service
- [x] payment-adapter-service
- [x] Kafka
- [ ] notification-service
- [ ] config-service
- [ ] discovery-service

## SERVICE RESPONSIBILITIES

1. auth-service

* User registration/login
* JWT authentication
* Refresh token flow
* BCrypt password hashing
* Basic RBAC (USER / ADMIN)

---

2. wallet-core-service
   Main business service.

Responsibilities:

* Wallet creation
* Wallet balance management
* Wallet-to-wallet transfer
* Transaction history
* Idempotency handling
* Ledger entry creation
* Transaction orchestration

Key concepts:

* PostgreSQL transactions
* Pessimistic locking
* Idempotency keys
* Immutable ledger entries
* Concurrent transfer safety

Important validations:

* Insufficient balance
* Self-transfer prevention
* Duplicate request prevention

---

3. payment-adapter-service
   Handles external payment gateway integration.

Responsibilities:

* Razorpay test-mode order creation
* Webhook signature verification
* Payment status mapping
* Payment event publishing

Key concepts:

* Adapter pattern
* Webhook verification
* External API isolation

---

4. notification-service
   Async event consumer.

Responsibilities:

* Consume Kafka events
* Handle WalletCredited event
* Handle TransferCompleted event
* Simulate email/SMS notifications

Key concepts:

* Kafka consumer groups
* Async processing

---

5. gateway-service
   Single entry point.

Responsibilities:

* Route requests
* JWT validation filter
* Redis-backed rate limiting
* Request logging

---

6. config-service
   Centralized configuration management.

---

7. discovery-service
   Service registry using Eureka.

---

## DATABASES & INFRA

Primary DB:

* PostgreSQL

Use PostgreSQL for:

* wallets
* transactions
* ledger entries
* idempotency records
* payment orders

Redis:

* rate limiting
* optional idempotency cache

Kafka:

* async event communication

Kubernetes:

* Deploy all services using K8s manifests

---



## MUST-HAVE CONCEPTS

* JWT authentication
* Refresh tokens
* PostgreSQL transactions
* Pessimistic locking
* Immutable ledger
* Idempotency keys
* Kafka async events
* Razorpay webhook verification
* Redis rate limiting
* Kubernetes deployment

---

## SKIP FOR NOW

* CQRS
* Saga orchestration engine
* Multi-currency support
* Fraud detection
* Complex KYC workflow
* ELK stack
* Prometheus/Grafana
* DLQ
* Service mesh
* Multiple payment providers
* OAuth2 login
* Advanced caching

---
