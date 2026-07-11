# FicMart Payment Gateway — Local Startup Guide

This guide explains how to start the payment gateway locally after reopening the project.

## 1. Prerequisites

Install and confirm:

- Java 21, or the Java version configured in the project
- Maven, or the included Maven Wrapper
- PostgreSQL
- Postman, Insomnia, or Thunder Client
- Docker Desktop only if the project depends on Docker services such as the mock bank API

Check Java:

```bash
java -version
```

Check Maven:

```bash
mvn -version
```

If the project includes Maven Wrapper, prefer:

### Windows

```powershell
./mvnw.cmd -version
```

### macOS/Linux

```bash
./mvnw -version
```

## 2. Open the project

Open the project root folder in IntelliJ IDEA or VS Code.

The root should contain:

```text
pom.xml
src/
mvnw
mvnw.cmd
```

## 3. Start PostgreSQL

Make sure PostgreSQL is running.

Expected database:

```text
payment_gateway
```

Typical local details:

```text
Host: localhost
Port: 5432
Database: payment_gateway
```

Create it once if it does not exist:

```sql
CREATE DATABASE payment_gateway;
```

## 4. Check application configuration

Open:

```text
src/main/resources/application.properties
```

Confirm the datasource values match your PostgreSQL setup:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_gateway
spring.datasource.username=YOUR_POSTGRES_USERNAME
spring.datasource.password=YOUR_POSTGRES_PASSWORD
spring.flyway.enabled=true
```

Avoid committing real passwords to GitHub.

If the project uses environment variables, make sure they are available before starting the application.

Possible names:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## 5. Start Docker services if required

If the project has `docker-compose.yml` or `compose.yml`, run this from the project root:

```bash
docker compose up -d
```

Check containers:

```bash
docker ps
```

View logs:

```bash
docker compose logs
```

Stop them later with:

```bash
docker compose down
```

Skip this section if PostgreSQL and the mock bank are not running through Docker.

## 6. Build the project

### Windows

```powershell
./mvnw.cmd clean install
```

### macOS/Linux

```bash
./mvnw clean install
```

Without Maven Wrapper:

```bash
mvn clean install
```

This downloads dependencies, compiles the project, runs tests, and checks the build.

For a quicker compile check:

```bash
mvn clean compile
```

## 7. Flyway migrations

Flyway normally runs automatically when Spring Boot starts.

Migration files belong in:

```text
src/main/resources/db/migration
```

Example:

```text
V1__create_payments_table.sql
```

If migration startup fails:

- Read the first Flyway error in the console
- Check the migration filename
- Check for conflicting tables or columns
- Do not edit an already-applied migration in a shared database
- Create a new migration for later schema changes

## 8. Start the Spring Boot application

### Windows

```powershell
./mvnw.cmd spring-boot:run
```

### macOS/Linux

```bash
./mvnw spring-boot:run
```

Without Maven Wrapper:

```bash
mvn spring-boot:run
```

You can also run the main application class from IntelliJ IDEA.

Typical local address:

```text
http://localhost:8080
```

## 9. Test authorize payment

Endpoint:

```text
POST http://localhost:8080/api/v1/payments/authorize
```

Header:

```text
Content-Type: application/json
```

Example body:

```json
{
  "orderId": "ORDER-1001",
  "customerId": "CUSTOMER-1001",
  "amountInCents": 250000,
  "currency": "NGN"
}
```

Expected result:

- A generated payment reference
- Status `PENDING`
- A success message

Copy the returned payment reference.

## 10. Test capture payment

Endpoint:

```text
POST http://localhost:8080/api/v1/payments/capture
```

Header:

```text
Content-Type: application/json
```

Example body:

```json
{
  "paymentReference": "PASTE_THE_REFERENCE_HERE"
}
```

Expected result:

- The same payment reference
- Status `CAPTURED`
- A success message

## 11. Test error handling

### Capture the same payment again

Expected:

```text
409 Conflict
```

Meaning:

```text
Payment has already been processed
```

### Capture a fake reference

Expected:

```text
404 Not Found
```

Meaning:

```text
Payment reference not found
```

### Send an invalid authorize request

Examples:

- Blank `orderId`
- Blank `customerId`
- Zero or negative `amountInCents`
- Blank `currency`

Expected:

```text
400 Bad Request
```

This requires `@Valid` together with `@RequestBody` in the controller.

## 12. Check the database

Run:

```sql
SELECT *
FROM payments
ORDER BY created_at DESC;
```

After authorization:

```text
PENDING
```

After capture:

```text
CAPTURED
```

## 13. Normal startup routine

After the initial setup:

1. Start PostgreSQL.
2. Start required Docker services.
3. Open the project.
4. Confirm environment variables.
5. Run Spring Boot.
6. Test authorize.
7. Use the returned reference to test capture.

You do not need `clean install` every time. Use it after dependency changes, migration changes, build issues, or before an important push.

## 14. Common problems

### Port 8080 is already in use

Stop the process using it, or use another port:

```properties
server.port=8081
```

### PostgreSQL connection refused

Check:

- PostgreSQL is running
- Port `5432` is correct
- Database name is correct
- Username and password are correct
- The Docker container is running, if PostgreSQL is containerized

### Flyway validation failed

Possible causes:

- An applied migration was edited
- Migration checksums changed
- A migration partially failed
- Database schema and Flyway history differ

Read the Flyway error before repairing or deleting migration history.

### Application returns 500

Check the console stack trace for:

- Database failures
- Null values
- Missing configuration
- Unhandled exceptions

### Validation does not run

Confirm the controller uses:

```text
@Valid
@RequestBody
```

Also confirm the validation dependency exists in `pom.xml`.

## 15. Shutdown

Stop Spring Boot with:

```text
Ctrl + C
```

Stop Docker services if used:

```bash
docker compose down
```

## Current payment flow

```text
Authorize payment
    ↓
Payment created with PENDING status
    ↓
Payment reference returned
    ↓
Capture request uses payment reference
    ↓
PENDING payment becomes CAPTURED
```

Current endpoints:

```text
POST /api/v1/payments/authorize
POST /api/v1/payments/capture
```

Current custom errors:

```text
PaymentRefNotFoundException → 404 Not Found
PaymentAlreadyProcessedException → 409 Conflict
```