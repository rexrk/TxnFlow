# TxnFlow — Reliable Payment & Wallet Backend

Brief

TxnFlow is a modular microservices reference implementation showcasing reliable payment and wallet patterns: idempotent APIs, immutable ledger accounting, concurrency-safe transfers, webhook verification, and asynchronous event-driven processing. Built for learning, experimentation, and as a solid foundation for production systems.

Core capabilities

- Idempotent request handling (Idempotency-Key support)
- Immutable ledger entries + transactional safety (Postgres)
- Concurrency controls (pessimistic locking where needed)
- Payment adapter with webhook signature verification (Razorpay-ready)
- Async eventing (Kafka) and notification consumer
- JWT-based auth, gateway with Redis rate-limiting, and service discovery
- Kubernetes manifests for deployment

Project structure

- auth-service/               — JWT auth, users, roles, refresh tokens
- wallet-service/             — wallets, balances, transfers, transactions, ledger, idempotency
- payment-adapter-service/    — external payment gateway adapter, webhook handling
- notification-service/       — Kafka consumer for email/SMS simulation and async notifications
- gateway-service/            — API gateway, routing, auth filter, logging, rate limiting (Redis)
- config-service/             — centralized configuration
- discovery-service/          — service registry (Eureka-style)
- k8s/                        — Kubernetes manifests and deployment examples
- docs/                       — architecture diagrams, sequence flows, failure cases, design notes
- README.md                   — this file

Infrastructure

- PostgreSQL: primary store for wallets, transactions, ledger, idempotency records
- Redis: rate-limiting and optional caching
- Kafka: event bus for async communication
- Kubernetes: deployment and scaling
- Optional: Cloudflare tunnel for local exposure during development

Quick start (overview)

1. Start core infra (Postgres, Redis, Kafka) via docker-compose or k8s
2. Configure environment variables using each service's README/sample.env
3. Start auth-service and gateway-service first, then wallet and adapters
4. Use Idempotency-Key header for safe retryable operations
5. Deploy to a Kubernetes cluster using manifests in k8s/

Development & testing

- Each service includes unit and integration tests; run per-service test scripts
- Use docs/ for architecture decisions and failure-mode tests

Contributing

Open issues or PRs. For large changes, open an issue first to discuss design. See docs/architecture.md for rationale.

License

Check LICENSE in the repo or contact the maintainer for licensing details.

---
