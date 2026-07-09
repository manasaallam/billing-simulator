# Billing Simulator

A Spring Boot REST API application for billing simulation.

## Tech Stack

- Java 17
- Spring Boot 3.3.1
- Spring Security (HTTP Basic)
- Spring Data JPA
- PostgreSQL
- Maven

## Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+

## Setup

1. **Create the PostgreSQL database:**

   ```sql
   CREATE DATABASE billing_simulator;
   ```

2. **Configure database credentials:**

   Set environment variables or update `application.yml`:
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```

3. **Build the project:**

   ```bash
   mvn clean install
   ```

4. **Run the application:**

   ```bash
   mvn spring-boot:run
   ```

   The API will be available at `http://localhost:8080`.

## API Endpoints

| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| GET    | /api/invoices                     | List all invoices        |
| GET    | /api/invoices/{id}                | Get invoice by ID        |
| POST   | /api/invoices                     | Create a new invoice     |
| PUT    | /api/invoices/{id}                | Update an invoice        |
| DELETE | /api/invoices/{id}                | Delete an invoice        |
| GET    | /api/invoices/customer/{name}     | Get invoices by customer |
| GET    | /api/invoices/status/{status}     | Get invoices by status   |

## Security

All endpoints require HTTP Basic authentication except `/api/public/**`.

Default credentials are configured via Spring Security. For development, you can disable security or add in-memory users in `SecurityConfig`.

## Running Tests

```bash
mvn test
```
