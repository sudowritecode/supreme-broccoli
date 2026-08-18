# Plinth API

Plinth API is the Spring Boot foundation for a privacy-first messaging platform. This initial scaffold accepts **client-encrypted message envelopes** and publishes them to a RabbitMQ outbound queue. It deliberately does not decrypt, inspect, or log message content.

> This is a backend foundation, not a complete secure messenger. Authentication, device-key lifecycle, audited end-to-end encryption protocol integration, persistence, delivery consumers, attachments, rate limiting, and production hardening remain to be implemented before a user-facing beta.

## Technology choices

| Area | Choice | Purpose |
|---|---|---|
| Runtime | Java 21 | Long-term-support JVM baseline |
| Framework | Spring Boot 3.4 | REST APIs, validation, operations endpoints, AMQP integration, and database migrations |
| Database | PostgreSQL (shared environments) / H2 (default local test runtime) | Durable account, device, consent, conversation, and encrypted-outbox records |
| Broker | RabbitMQ 4 management image | Durable local broker, delivery queue, dead-letter queue, and development console |
| Build | Maven Wrapper | Reproducible dependency and test workflow |

## Run locally

The default application profile uses an in-memory H2 datastore and starts without broker publishing. It is appropriate for API development and automated tests only.

```bash
./mvnw spring-boot:run
```

For a durable local PostgreSQL datastore and message broker, start the Compose dependencies, then activate the `postgres,broker` profiles.

```bash
cp .env.example .env
docker compose up -d postgres rabbitmq
docker compose ps

export RABBITMQ_USERNAME=plinth
export RABBITMQ_PASSWORD=plinth-local-dev
export DATABASE_USERNAME=plinth
export DATABASE_PASSWORD=plinth-local-dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres,broker
```

The RabbitMQ management console is available at `http://localhost:15672` with the local development credentials from `.env.example`.

## Verify the scaffold

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/platform/status
```

## MVP identity workflow

The first product vertical slice establishes a test account and its initial device. The API returns a one-time **development access token** after registration; the backend keeps only its SHA-256 hash. Supply that token on every authenticated endpoint in the `X-Device-Token` header.

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "alice",
    "deviceLabel": "Alice Android",
    "publicIdentityKey": "base64-encoded-public-identity-key"
  }'
```

The current message-ingress contract is intentionally narrow. It accepts an opaque ciphertext value and returns `202 Accepted`; authentication and conversation authorization are required before the API queues any event. The backend derives the sender device from the access token instead of accepting it from the caller.

```bash
curl -X POST http://localhost:8080/api/v1/messages \
  -H 'Content-Type: application/json' \
  -H 'X-Device-Token: REPLACE_WITH_DEVELOPMENT_TOKEN' \
  -d '{
    "messageId": "11111111-1111-1111-1111-111111111111",
    "conversationId": "22222222-2222-2222-2222-222222222222",
    "recipientDeviceId": "44444444-4444-4444-4444-444444444444",
    "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
  }'
```

With the `broker` profile active, use the RabbitMQ management console to inspect the `plinth.message.outbound` queue. The next implementation slice adds the delivery worker; a published encrypted event remains in that queue until a consumer is introduced.

## Test and package

```bash
./mvnw test
./mvnw package
```

## Repository layout

```text
src/main/java/za/hungu/plinth/
  api/          HTTP contracts and controllers
  auth/         Device-token authentication and authorization boundaries
  identity/     Accounts, devices, and public-key registration
  contacts/     Explicit contact-consent state and requests
  conversation/ Direct conversation membership
  outbox/       Transactional encrypted-message delivery records
  config/       Typed configuration and RabbitMQ topology
  messaging/    Ciphertext event contract and broker abstraction
  health/       Lightweight platform state endpoint
src/main/resources/db/migration/
                Versioned Flyway migrations
src/test/        Context, API, and vertical-slice tests
docs/            Architecture and operational documentation
compose.yaml     Local PostgreSQL and RabbitMQ dependencies
```

## Security boundary

The broker payload is an `EncryptedMessageEvent`. The server treats `ciphertext` as opaque transport data and must never accept a plaintext alternative. The present scaffold should not be described as Signal-compatible or secure messaging until it integrates an audited encryption protocol, device-key verification, authentication, authorization, cryptographic storage, delivery-consumer controls, security testing, and independent review.

Read [`docs/architecture/messaging.md`](docs/architecture/messaging.md) before extending the message flow.
