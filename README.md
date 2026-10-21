# Payment System

Portfolio backend project built with Java and Spring Boot. It models a small payment API with clients, accounts and money movements.

> Personal learning project. It is not a real payment processor and must not be treated as production financial software.

## What this project demonstrates

- REST API design with Spring Boot
- PostgreSQL persistence with Spring Data JPA
- Flyway database migrations
- JWT authentication and BCrypt password hashing
- Ownership checks for account operations
- Deposits, withdrawals and account-to-account transfers
- Idempotency keys for retry-safe money operations
- Pessimistic locking for concurrent balance updates
- Rate limiting with Bucket4j
- Paginated and filterable transaction history
- Structured logging / request correlation
- Unit and integration tests, including concurrency scenarios
- Testcontainers with PostgreSQL
- Docker multi-stage build and Docker Compose

## Scope

This repository intentionally focuses on the backend payment domain. A previous experimental fraud-feature integration was removed so the repository remains a small, complete project. Fraud detection and MLOps live separately in the `mlops-platform` project.

## Main endpoints

Public:

- `POST /api/clients` — register a client
- `POST /api/auth/login` — authenticate and receive a JWT

Authenticated:

- `POST /api/accounts?clientId={id}` — create the authenticated client's account
- `GET /api/accounts/{id}` — read an owned account
- `GET /api/accounts/{id}/transactions` — paginated/filterable transaction history
- `POST /api/accounts/{id}/deposit`
- `POST /api/accounts/{id}/withdraw`
- `POST /api/accounts/{id}/transfer`

Money-operation endpoints require an `Idempotency-Key` header.

## Run with Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Replace the example values in `.env`, especially `JWT_SECRET`, before starting.

The API is available on `http://localhost:8080`.

## Run tests

```bash
./mvnw test
```

Integration tests use Testcontainers, so Docker must be running.

## Design decisions worth discussing in an interview

### Idempotency
A unique idempotency key prevents retries from applying the same balance-changing operation twice.

### Concurrency
Balance-changing operations use database locking. Transfers lock both accounts in deterministic ID order to reduce lost updates and deadlock risk.

### Database migrations
Hibernate uses `ddl-auto=validate`; Flyway owns schema creation and evolution.

### Security
Passwords are hashed with BCrypt, protected endpoints use stateless JWT authentication, and runtime secrets come from environment variables.

## Intentionally out of scope

- Real banking/payment-network integrations
- PCI-DSS compliance
- Distributed microservices
- Fraud ML integration
- Frontend/UI
