![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

# Ficmart Payment Gateway

A production-inspired Spring Boot payment gateway that processes payment authorizations, captures, voids, and refunds while ensuring safe retries through idempotency. The gateway integrates with a mock bank service, persists payment state in PostgreSQL, and includes integration tests covering the complete payment lifecycle.

---

# Live Demo

**Swagger UI**

http://18.200.250.3:8080/swagger-ui/index.html

**Health Check**

http://18.200.250.3:8080/actuator/health

> Hosted on AWS EC2 using Docker and PostgreSQL.

---

## Features

- Authorize payments
- Capture authorized payments
- Void authorized payments
- Refund captured payments
- Retrieve payments by payment reference
- Retrieve payment history by order ID
- Retrieve payment history by customer ID
- Idempotent request handling using the `Idempotency-Key` header
- OpenAPI/Swagger documentation
- Flyway database migrations
- Docker support
- Production deployment on AWS EC2
- Integration tests covering successful flows and failure scenarios

---

## Technology Stack

- Java 21
- Spring Boot 4
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- Maven
- Docker & Docker Compose
- JUnit 5
- MockMvc
- AWS EC2

---

## Project Structure

```text
src/main/java
├── api
│   ├── controller
│   ├── dto
│   └── exception
├── application
│   ├── mapper
│   └── service
├── domain
│   ├── entity
│   ├── repository
│   └── enums
└── infrastructure
    ├── bank
    └── configuration
```

---

## Payment Lifecycle

```text
               +-----------+
               | PENDING   |
               +-----------+
                     |
                     ▼
              +--------------+
              | AUTHORIZED   |
              +--------------+
                |          |
                |          ▼
                |      +--------+
                |      | VOIDED |
                |      +--------+
                ▼
          +--------------+
          | CAPTURED     |
          +--------------+
                |
                ▼
          +--------------+
          | REFUNDED     |
          +--------------+
```

Invalid state transitions are rejected with appropriate error responses.

---

## Idempotency

Every payment operation requires an `Idempotency-Key` request header.

The gateway:

- Returns the original response when the same request is retried with the same idempotency key.
- Returns **409 Conflict** when the same key is reused with a different request body.
- Prevents duplicate payment operations caused by retries or network failures.

---

## API Endpoints

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/payments/authorize` | Authorize a payment |
| POST | `/api/v1/payments/capture` | Capture an authorized payment |
| POST | `/api/v1/payments/void` | Void an authorized payment |
| POST | `/api/v1/payments/refund` | Refund a captured payment |
| GET | `/api/v1/payments/{paymentReference}` | Retrieve a payment |
| GET | `/api/v1/payments/order/{orderId}` | Retrieve payments by order ID |
| GET | `/api/v1/payments/customer/{customerId}` | Retrieve payments by customer ID |

---

# Running the Project

## Option 1 — Run Without Docker

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Docker (for the mock bank)

### Clone the Repository

```bash
git clone https://github.com/Copstud3/PayWall.git
cd PayWall
```

### Configure Environment

Create a `.env` file:

```text
DB_URL=jdbc:postgresql://localhost:5432/payment_gateway
DB_USERNAME=postgres
DB_PASSWORD=your_password

BANK_BASE_URL=http://localhost:8787
```

### Start PostgreSQL

Start your local PostgreSQL instance.

### Run Database Migrations

```bash
mvn flyway:migrate
```

### Start the Mock Bank

Run the mock bank using its Docker Compose configuration.

### Start the Application

```bash
mvn spring-boot:run
```

Application:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Health:

```text
http://localhost:8080/actuator/health
```

---

## Option 2 — Run With Docker

### Prerequisites

- Docker
- Docker Compose

### Configure Environment

Create a `.env` file:

```text
PAYMENT_DB_USERNAME=payment_user
PAYMENT_DB_PASSWORD=your_password

BANK_BASE_URL=http://host.docker.internal:8787
```

### Build and Start

```bash
docker compose up --build
```

Detached mode:

```bash
docker compose up -d --build
```

### Stop

```bash
docker compose down
```

Remove volumes:

```bash
docker compose down -v
```

Application:

```text
http://localhost:8080
```

---

## Deploying to AWS EC2

Clone the repository on your EC2 instance:

```bash
git clone https://github.com/Copstud3/PayWall.git
cd PayWall
```

Create your production `.env` file.

Deploy using:

```bash
./deploy.sh
```

The deployment script:

- Pulls the latest code
- Builds Docker images
- Restarts containers
- Waits for the application health check
- Displays deployment status

---

## Docker Architecture

```text
                    Client
                       │
                       ▼
               Port 8080 (EC2)
                       │
                       ▼
              +--------------------+
              | Payment Gateway    |
              +---------+----------+
                        |
          +-------------+-------------+
          |                           |
          ▼                           ▼
+----------------------+      +----------------------+
| Payment PostgreSQL   |      | Mock Bank API        |
+----------------------+      +----------+-----------+
                                         |
                                         ▼
                             +----------------------+
                             | Mock Bank PostgreSQL |
                             +----------------------+
```

---

## Useful Docker Commands

Build:

```bash
docker compose build
```

Start:

```bash
docker compose up
```

Detached:

```bash
docker compose up -d
```

Logs:

```bash
docker compose logs -f
```

Gateway logs:

```bash
docker compose logs -f payment-gateway
```

Stop:

```bash
docker compose down
```

---

## API Documentation

Local:

```text
http://localhost:8080/swagger-ui/index.html
```

Production:

```text
http://18.200.250.3:8080/swagger-ui/index.html
```

---

## Health Endpoint

Local:

```text
http://localhost:8080/actuator/health
```

Production:

```text
http://18.200.250.3:8080/actuator/health
```

---

## Running Tests

Run all tests:

```bash
mvn test
```

Full verification:

```bash
mvn clean verify
```

---

## Error Handling

The gateway returns appropriate HTTP status codes for common scenarios, including:

- Request validation failures
- Missing idempotency key
- Payment not found
- Invalid payment state
- Idempotency conflicts
- Bank communication failures
- External bank operation failures

---

## Design Highlights

- Layered architecture separating API, application, domain, and infrastructure concerns.
- Payment lifecycle enforced through business rules.
- Payment state stored independently from payment operation history.
- MapStruct used for request and response mapping.
- Flyway manages database schema migrations.
- Integration tests validate the complete payment lifecycle and idempotency behavior.
- Dockerized development and deployment environment.
- Production deployment on AWS EC2.
- Automated deployments using `deploy.sh`.

---

## Future Improvements

- Optimistic locking for concurrent updates
- Formal payment state machine
- Circuit breaker and retry policies
- Scheduled reconciliation jobs
- Testcontainers for integration testing
- Metrics and distributed tracing
- Authentication and authorization
- PCI-compliant card tokenization
- CI/CD pipeline with GitHub Actions