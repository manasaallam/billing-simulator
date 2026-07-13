# Billing Simulation Agent

Real-time, self-service billing simulation that lets customers explore how shipping decisions impact billing outcomes — powered by Gemini AI for natural-language understanding and business-friendly explanations.

---

## Authentication Backend (current implementation)

> The sections below this one describe the broader product vision. What is
> actually implemented in this repository today is a minimal **authentication
> backend** for the React frontend.

- Package root: `com.ups.billing`
- **No database.** Users are stored in a thread-safe in-memory
  `ConcurrentHashMap` keyed by email. All data is lost on restart.
- **Spring Security** is used **only** for `BCryptPasswordEncoder` password
  hashing — the default security filter chain / login form is disabled.
- **Port:** `8080`
- **CORS:** allows the Vite frontend origin `http://localhost:5173`
  (`POST`, `OPTIONS`, `Content-Type`).

### How to run

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

#### Seeded demo user

A demo user is seeded on startup so you can log in immediately:

| Field     | Value               |
|-----------|---------------------|
| name      | `Demo Customer`     |
| email     | `demo@customer.com` |
| password  | `demo1234`          |
| accessKey | `ACME2026`          |
| role      | `CUSTOMER`          |

### POST `/api/auth/signup`

Request:

```json
{
  "name": "string",
  "accessKey": "string",
  "email": "string",
  "password": "string",
  "confirmPassword": "string"
}
```

Validation (`400` with `fieldErrors`):

- `name` — required, not blank.
- `accessKey` — required, alphanumeric only (`^[A-Za-z0-9]+$`).
- `email` — required, valid email format.
- `password` — required, minimum 8 characters.
- `confirmPassword` — required, must equal `password`.

Business rules:

- Duplicate email → `409 Conflict`, message
  `"An account with this email already exists."`
- Success → `201 Created` with the `AuthResponse` payload (below). The password
  is BCrypt-hashed and a random `userId` / `accountId` are generated;
  `role = "CUSTOMER"`.

### POST `/api/auth/login`

Request:

```json
{ "email": "string", "password": "string" }
```

Validation (`400` with `fieldErrors`):

- `email` — required, valid email format.
- `password` — required, not blank.

Business rules:

- Unknown email **or** password mismatch → `401 Unauthorized`, message
  `"Invalid email or password."` (does not reveal which field is wrong).
- Success → `200 OK` with the `AuthResponse` payload.

### Success payload (`AuthResponse`)

```json
{
  "userId": "...",
  "accountId": "...",
  "name": "...",
  "email": "...",
  "role": "CUSTOMER"
}
```

The password hash is **never** returned.

### Error payload (`ApiError`)

```json
{
  "message": "human readable summary",
  "fieldErrors": { "email": "Enter a valid email address." }
}
```

HTTP statuses: `400` validation, `401` bad credentials, `409` duplicate email,
`500` fallback.

### Example curl commands

Signup:

```bash
curl -i -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ada Lovelace",
    "accessKey": "ACME2026",
    "email": "ada@example.com",
    "password": "supersecret",
    "confirmPassword": "supersecret"
  }'
```

Login (seeded demo user):

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@customer.com",
    "password": "demo1234"
  }'
```

---

## Architecture

```
React UI
    │
    ▼
Spring Boot APIs (Orchestrator)
    │
    ├──► Gemini (Vertex AI) — NL parameter extraction
    ├──► BigQuery — Customer historical baseline (last 12 months)
    ├──► Java Rate Engine — Deterministic billing simulation
    ├──► Vector Search — Relevant policies & rate guides
    │
    ▼
Gemini (Vertex AI) — Business-language explanation
    │
    ▼
React Dashboard (results + explanation + confidence)
```

## Project Structure

```
src/main/java/com/example/billingsimulator/
│
├── BillingSimulatorApplication.java          # Spring Boot entry point
│
├── controller/
│   ├── InvoiceController.java                # CRUD endpoints for invoices
│   └── SimulationController.java             # POST /api/simulate — main simulation endpoint
│                                             # POST /api/simulate/clarify — submit clarification answers
│
├── service/
│   ├── SimulationOrchestrator.java           # Central coordinator — wires all services together
│   │                                         #   1. Extract → 2. Validate → 3. Baseline → 4. Simulate → 5. Explain → 6. Audit
│   │
│   ├── ParameterExtractionService.java       #Gemini NL → structured SimulationParameters
│   ├── ParameterValidationService.java       #  Validate params, return clarification questions if ambiguous
│   │
│   ├── BaselineService.java                  # Fetch customer's 12-month history from BigQuery
│   ├── RateEngineService.java                # Run existing Java rate engine with injected scenario parameters
│   ├── ExplanationService.java               # Gemini: simulation results → business-friendly explanation
│   ├── PolicySearchService.java              # Vector Search: retrieve relevant rate policies for RAG context
│   ├── AuditService.java                     # Persist audit trail (inputs, parameters, baseline, results)
│   └── InvoiceService.java                   # CRUD operations for invoices
│
├── model/
│   ├── SimulationRequest.java                # Incoming request: customerId + NL query + conversationId
│   ├── SimulationParameters.java             # Structured params extracted from NL
│   ├── ServiceShift.java                     # "Move X% from ServiceA to ServiceB"
│   ├── VolumeChange.java                     # "Increase/decrease volume by X packages or Y%"
│   ├── PackageProfile.java                   # "Change avg weight/dimensions"
│   ├── CustomerBaseline.java                 # Historical data: shipments, spend, discounts by service level
│   ├── SimulationResult.java                 # Rate engine output: current vs. simulated cost, savings, confidence
│   ├── SimulationResponse.java               # API response: result OR clarification questions + disclaimer
│   ├── ClarificationQuestion.java            # Follow-up question when params are ambiguous
│   ├── SimulationAuditLog.java               # JPA entity for audit persistence
│   └── Invoice.java                          # Invoice entity
│
├── repository/
│   ├── InvoiceRepository.java                # JPA repo for invoices
│   └── SimulationAuditLogRepository.java     # JPA repo for simulation audit logs
│
├── config/
│   ├── SecurityConfig.java                   # Spring Security — permits simulation endpoints
│   ├── CorsConfig.java                       # CORS for React frontend (localhost:3000)
│   ├── GeminiConfig.java                     # Vertex AI / Gemini client configuration
│   └── BigQueryConfig.java                   # BigQuery client and cost control settings
│
└── exception/
    ├── ClarificationRequiredException.java   # Thrown when params need clarification
    └── GlobalExceptionHandler.java           # Maps exceptions to structured JSON responses
```

## Simulation Flow

```
Customer: "What if I shift 20% of Express to Ground?"
                         │
                         ▼
         ┌─ ParameterExtractionService (Gemini) ──┐
         │  Extracts: {from: Express,              │
         │             to: Ground, pct: 20%}       │
         └─────────────────────────────────────────┘
                         │
                         ▼
         ┌─ ParameterValidationService ────────────┐
         │  ✓ Service levels valid                  │
         │  ✓ Percentage in range                   │
         │  ✗ Missing? → ClarificationQuestion      │
         └─────────────────────────────────────────┘
                         │
                         ▼
         ┌─ BaselineService (BigQuery) ────────────┐
         │  Last 12 months: 10K Express, 5K Ground │
         │  Avg weight: 8lb, Monthly spend: $35K   │
         └─────────────────────────────────────────┘
                         │
                         ▼
         ┌─ RateEngineService ─────────────────────┐
         │  Current: $35,000/mo                     │
         │  Simulated: $30,800/mo                   │
         │  Savings: $4,200 (12%)                   │
         └─────────────────────────────────────────┘
                         │
                         ▼
         ┌─ ExplanationService (Gemini) ───────────┐
         │  "Shifting 20% of Express to Ground     │
         │   could save ~$4,200/month (12%)..."     │
         └─────────────────────────────────────────┘
                         │
                         ▼
         ┌─ AuditService ─────────────────────────┐
         │  Log: query, params, baseline, result   │
         └─────────────────────────────────────────┘
```

## Team Ownership

| Component | Owner | Status |
|-----------|-------|--------|
| ParameterExtractionService | **Your team** | To implement |
| ParameterValidationService | **Your team** | To implement |
| SimulationOrchestrator | Shared | To implement |
| BaselineService | Team B | To implement |
| RateEngineService | Team C | To implement |
| ExplanationService | Team D | To implement |
| PolicySearchService | Team D | To implement |
| AuditService | Shared | To implement |
| React Frontend | Frontend team | To implement |

## Tech Stack

- Java 17
- Spring Boot 3.3.1
- Spring Security
- Spring Data JPA
- Gemini (Vertex AI)
- Google BigQuery
- Vector Search
- H2 (dev) / PostgreSQL (prod)
- Maven

## Running Locally

```bash
# Uses H2 in-memory database (dev profile active by default)
mvn spring-boot:run

# App starts at http://localhost:8080
# H2 console at http://localhost:8080/h2-console
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/simulate` | Submit NL query for billing simulation |
| POST | `/api/simulate/clarify` | Submit clarification answers |
| GET | `/api/invoices` | List all invoices |
| GET | `/api/invoices/{id}` | Get invoice by ID |
| POST | `/api/invoices` | Create a new invoice |
| PUT | `/api/invoices/{id}` | Update an invoice |
| DELETE | `/api/invoices/{id}` | Delete an invoice |
| GET | `/api/invoices/customer/{name}` | Get invoices by customer |
| GET | `/api/invoices/status/{status}` | Get invoices by status |

## Key Considerations

- **Projection Framing**: Results are projections, not quotes — confidence indicators and disclaimers included
- **Input Validation**: Ambiguous queries trigger clarification questions instead of assumed simulations
- **Cost Control**: BigQuery uses partition pruning + caching to minimize query costs
- **Auditability**: Every simulation logged with inputs, parameters, baseline, and results
- **Security**: Access control on simulation endpoints; sensitive pricing data protected

## Running Tests

```bash
mvn test
```
