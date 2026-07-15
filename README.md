# Ficmart Payment Gateway

A Spring Boot payment gateway that processes payment authorizations, captures, voids, and refunds while ensuring safe retries through idempotency. The gateway integrates with a mock bank service and persists payment state in PostgreSQL.

## Features

* Authorize payments
* Capture authorized payments
* Void authorized payments
* Refund captured payments
* Retrieve payments by payment reference
* Retrieve payment history by order ID
* Retrieve payment history by customer ID
* Request idempotency using the `Idempotency-Key` header
* OpenAPI/Swagger documentation
* Flyway database migrations
* Integration tests covering happy paths and common failure scenarios

## Technology Stack

* Java 21
* Spring Boot 4
* Spring Data JPA
* PostgreSQL
* Flyway
* MapStruct
* Lombok
* Maven
* Docker
* JUnit 5
* MockMvc

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

## Payment Lifecycle

The gateway supports the following payment flow:

```text
PENDING
   │
   ▼
AUTHORIZED
   ├────────────► VOIDED
   │
   ▼
CAPTURED
   │
   ▼
REFUNDED
```

Invalid state transitions are rejected with an appropriate error response.

## Idempotency

Every payment operation requires an `Idempotency-Key` request header.

The gateway:

* Returns the original response when the same request is retried with the same key.
* Returns `409 Conflict` if the same key is reused with a different request body.
* Prevents duplicate payment operations caused by client retries or network failures.

## API Endpoints

| Method | Endpoint                                 | Description                      |
| ------ | ---------------------------------------- | -------------------------------- |
| POST   | `/api/v1/payments/authorize`             | Authorize a payment              |
| POST   | `/api/v1/payments/capture`               | Capture an authorized payment    |
| POST   | `/api/v1/payments/void`                  | Void an authorized payment       |
| POST   | `/api/v1/payments/refund`                | Refund a captured payment        |
| GET    | `/api/v1/payments/{paymentReference}`    | Retrieve a payment               |
| GET    | `/api/v1/payments/order/{orderId}`       | Retrieve payments by order ID    |
| GET    | `/api/v1/payments/customer/{customerId}` | Retrieve payments by customer ID |

## Running the Project

### Prerequisites

* Java 21
* Maven
* PostgreSQL
* Docker (for the mock bank)

### Clone the Repository

```bash
git clone https://github.com/Copstud3/PayWall
cd ficmart-payment-gateway
```

### Configure Environment

Create a `.env` file containing:

```text
DB_URL=jdbc:postgresql://localhost:5432/payment_gateway
DB_USERNAME=postgres
DB_PASSWORD=your_password

BANK_BASE_URL=http://localhost:8787
```

Update the values to match your local environment.

### Start PostgreSQL

Start your PostgreSQL instance.

### Run Database Migrations

```bash
mvn flyway:migrate
```

### Start the Mock Bank

```bash
docker compose up
```

### Start the Application

```bash
mvn spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

or

```text
http://localhost:8080/swagger-ui/index.html
```

depending on your SpringDoc configuration.

## Running Tests

Run all tests:

```bash
mvn test
```

Run a clean build:

```bash
mvn clean verify
```

## Error Handling

The gateway returns appropriate HTTP status codes for common scenarios, including:

* Invalid request validation
* Missing idempotency key
* Payment not found
* Invalid payment state
* Idempotency conflicts
* Bank communication failures

## Design Highlights

* Layered architecture separating API, business logic, domain, and infrastructure.
* MapStruct used for request and response mapping.
* Flyway manages database schema changes.
* Payment state stored independently from payment operation history.
* Integration tests validate core payment flows and idempotency behavior.

## Future Improvements

* Optimistic locking for concurrent updates
* Formal payment state machine
* Circuit breaker and retry policies
* Reconciliation jobs for partial failures
* Testcontainers for integration testing
* Metrics, tracing, and monitoring
* Authentication and authorization
* PCI-compliant card tokenization

## License

This project was developed as a technical assessment and portfolio project.
