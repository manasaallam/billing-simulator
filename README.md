# Billing Simulator

A billing simulation agent that allows customers to ask **"what-if" questions** about their shipping costs in natural language. The system projects the financial impact of changing volume, service levels, fuel prices, package profiles, zones, and accessorials. It also explains invoice charges via an AI-powered chatbot.

> **Core Philosophy:** *The AI never computes money. The Java rate engine does all math.*
> - AI layer (Gemini / local rules) extracts parameters and phrases answers
> - Java backend computes all financial projections (auditable, deterministic)
> - Results are always presented as **projections, not guaranteed quotes**

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Database](#database)
- [API Endpoints](#api-endpoints)
- [Architecture](#architecture)
- [Rate Calculation Formula](#rate-calculation-formula)
- [Simulation Scenarios](#simulation-scenarios)
- [AI Integration](#ai-integration)
- [Knowledge Base](#knowledge-base)
- [Testing](#testing)
- [Design Decisions](#design-decisions)

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.3.1 | Application framework |
| Spring Data JPA | 3.3.1 | Database ORM |
| Spring Security | 3.3.1 | Authentication & CORS |
| Spring WebFlux | 3.3.1 | Reactive HTTP client (Gemini API calls) |
| PostgreSQL | 15 | Production database (Supabase) |
| H2 | — | In-memory database (dev/test) |
| Maven | — | Build tool |
| Gemini 2.0 Flash | — | AI parameter extraction & NL responses |

---

## Project Structure

```
billing-simulator/
├── pom.xml
├── README-DESIGN.md                  # Architecture & design document
├── Requirements.md                   # Invoice samples & contract references
├── INPUT-PARAMS-REQUEST.md           # Full API reference (request/response shapes)
├── database/
│   ├── README-DDL.md                 # Schema documentation
│   ├── README-DML.md                 # Seed data documentation
│   ├── ddl/
│   │   └── 01_schema.sql            # PostgreSQL DDL
│   └── dml/
│       └── 02_seed_data.sql          # Demo account, rates & tiers
├── src/
│   ├── main/
│   │   ├── java/com/example/billingsimulator/
│   │   │   ├── BillingSimulatorApplication.java
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── InvoiceExplainerController.java
│   │   │   │   ├── RateController.java
│   │   │   │   ├── RateSimulationController.java
│   │   │   │   └── SimulationController.java
│   │   │   ├── exception/
│   │   │   │   ├── ContractNotFoundException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidInputException.java
│   │   │   │   └── RateNotFoundException.java
│   │   │   ├── model/              # 30+ entity & DTO classes
│   │   │   ├── repository/         # 12 Spring Data JPA interfaces
│   │   │   └── service/
│   │   │       ├── InvoiceExplainerService.java
│   │   │       ├── InvoiceKnowledgeService.java
│   │   │       ├── ParameterExtractionService.java
│   │   │       ├── ParameterValidationService.java
│   │   │       ├── RateEngineService.java
│   │   │       ├── SimulationService.java
│   │   │       └── ai/
│   │   │           ├── AiClient.java
│   │   │           ├── GeminiAiClient.java
│   │   │           └── LocalRuleBasedAiClient.java
│   │   └── resources/
│   │       ├── application.yml           # Supabase PostgreSQL config
│   │       ├── application-dev.yml       # H2 in-memory for dev
│   │       └── knowledge/
│   │           ├── charge-explanations.json
│   │           ├── fuel-schedule.json
│   │           └── surcharge-rules.json
│   └── test/
│       ├── java/com/example/billingsimulator/
│       │   └── BillingSimulatorApplicationTests.java
│       └── resources/
│           └── application-test.yml      # H2 for tests
```

---

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- (Optional) **Gemini API Key** from [AI Studio](https://aistudio.google.com/apikey)

### Run Locally (Dev Profile — H2 In-Memory DB)

```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

- Application starts at `http://localhost:8080`
- H2 Console available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:billing_simulator`)

### Run with Supabase (Production)

```bash
export SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
export SUPABASE_DB_USER=postgres
export SUPABASE_DB_PASSWORD=<password>
mvn spring-boot:run
```

### Enable Gemini AI

```bash
export GEMINI_API_KEY=<your_api_key>
mvn spring-boot:run
```

Without a Gemini API key, the application falls back to a **local rule-based AI client** that uses regex patterns to extract parameters — no external API calls needed.

### Build

```bash
mvn clean package
```

---

## Configuration

| Profile | Database | AI Client | Use Case |
|---|---|---|---|
| `dev` | H2 in-memory | LocalRuleBasedAiClient | Local development |
| `test` | H2 (create-drop) | LocalRuleBasedAiClient | Automated tests |
| *(default)* | Supabase PostgreSQL | GeminiAiClient (if API key set) | Production |

### Key Properties

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Application port |
| `gemini.api-key` | — | Gemini API key (enables real NL extraction) |
| `gemini.model` | `gemini-2.0-flash` | Gemini model to use |
| `spring.jpa.hibernate.ddl-auto` | `none` | Schema managed via SQL scripts |

### Security

- **CORS:** Allows `localhost:3000` (React) and `localhost:5173` (Vite)
- **Session:** Stateless (JWT)
- **CSRF:** Disabled
- **Public endpoints:** `/api/simulate/**`, `/api/rate/**`, `/api/explain/**`, `/actuator/health`

---

## Database

**Engine:** PostgreSQL 15 (Supabase) | H2 (dev/test)  
**Schema management:** Manual SQL scripts (`ddl-auto: none`)

### Table Groups (22 tables)

| Group | Tables | Purpose |
|---|---|---|
| **Identity** | `account`, `app_user` | Customer scope & login |
| **Rate Reference** | `service_level`, `zone_matrix`, `rate_card`, `dim_factor`, `min_shipping_charge` | Published rate tables |
| **Fuel** | `fuel_program`, `fuel_index` | Weekly diesel index → fuel surcharge % |
| **Accessorials** | `accessorial_type` | Surcharge rule cards |
| **Pricing** | `discount_category`, `pricing_program`, `discount_tier` | Volume-tiered discount bands |
| **Contract** | `contract`, `contract_incentive` | Customer pricing & incentives |
| **Billing** | `payment_plan`, `payment_plan_account`, `customer_profile` | Invoice preferences |
| **Baseline** | `shipment`, `shipment_charge`, `baseline_snapshot` | 12-month history & aggregates |
| **Chat / Audit** | `conversation`, `chat_message`, `simulation_scenario` | Conversation logs & audit trail |
| **Invoice** | `invoice`, `invoice_line` | Actual & projected invoices |
| **Knowledge** | `knowledge_article` | RAG metadata |

### Five Discount Categories

Instead of individual service codes, discounts are grouped into 5 categories:

| Category | Service Codes |
|---|---|
| `GROUND` | GROUND, GROUND_RES, THREE_DAY |
| `AIR` | EXPRESS, EXPRESS_SAVER, TWO_DAY |
| `INTL_STANDARD` | International Standard |
| `INTL_EXPRESS_EXPORT` | International Express Export |
| `INTL_EXPRESS_IMPORT` | International Express Import |

### Volume-Tiered Discount Bands (Demo: "Save as You Grow")

| Weekly Volume | GROUND | AIR | INTL_EXP_EXPORT | INTL_EXP_IMPORT | INTL_STANDARD |
|---|---|---|---|---|---|
| 0–10 | 36% | 60% | 55% | 47% | 27% |
| 11–30 | 44% | 66% | 65% | 53% | 32% |
| 31–40 | 46% | 68% | 70% | 54% | 33% |
| 41–50 | 50% | 72% | 71% | 55% | 34% |
| 51–70 | 51% | 74% | 72% | 56% | 35% |
| 71+ | 52% | 75% | 74% | 58% | 37% |

For full schema and seed data details, see `database/README-DDL.md` and `database/README-DML.md`.

---

## API Endpoints

### Rate Quoting — `/api/rate`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/rate/quote` | Single-package rate quote |
| `POST` | `/api/rate/quote/batch` | Batch quote (up to 50 packages) |
| `GET` | `/api/rate/card` | List available rate card versions |

### Rate Simulation — `/api/rate/simulate`

| Method | Path | Scenario |
|---|---|---|
| `POST` | `/api/rate/simulate/volume-change` | Volume increase/decrease |
| `POST` | `/api/rate/simulate/service-shift` | Shift % between service levels |
| `POST` | `/api/rate/simulate/package-profile` | Weight/dimension changes |
| `POST` | `/api/rate/simulate/zone-mix` | Zone distribution changes |
| `POST` | `/api/rate/simulate/accessorial` | Add/remove surcharges |
| `POST` | `/api/rate/simulate/fuel-change` | Fuel surcharge % change |
| `POST` | `/api/rate/simulate/combined` | Multiple changes at once |
| `POST` | `/api/rate/simulate/optimize` | Find next tier threshold |
| `POST` | `/api/rate/simulate/compare` | Side-by-side scenario comparison |

### AI-Powered Simulation — `/api/simulate`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/simulate` | Natural language query → parameter extraction → simulation |
| `POST` | `/api/simulate/clarify` | Answer clarification questions and re-run |

### Invoice Explainer — `/api/explain`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/explain` | Explain invoice charges in natural language |

For complete request/response shapes with examples, see `INPUT-PARAMS-REQUEST.md`.

---

## Architecture

### End-to-End Request Flow

```
┌──────────┐     POST /api/simulate          ┌─────────────────────┐
│ React UI │ ──────────────────────────────── │ SimulationController│
└──────────┘  { contractId, NL query }        └─────────┬───────────┘
                                                        │
                                              ┌─────────▼───────────┐
                                              │ ParameterExtraction │
                                              │     Service         │
                                              │  (Gemini / Local)   │
                                              └─────────┬───────────┘
                                                        │ SimulationParameters
                                              ┌─────────▼───────────┐
                                              │ ParameterValidation │
                                              │     Service         │
                                              └─────────┬───────────┘
                                                        │ valid? ──── no ──→ ClarificationQuestions
                                              ┌─────────▼───────────┐
                                              │  SimulationService  │
                                              │  (dispatch by type) │
                                              └─────────┬───────────┘
                                                        │
                                              ┌─────────▼───────────┐
                                              │  RateEngineService  │
                                              │ (all money math)    │
                                              └─────────┬───────────┘
                                                        │
                                              ┌─────────▼───────────┐
                                              │ SimulationResponse  │
                                              │ { status, result,   │
                                              │   disclaimer }      │
                                              └─────────────────────┘
```

### Service Layer

| Service | Responsibility |
|---|---|
| **RateEngineService** | All deterministic money calculations (zone lookup, weight resolution, rate lookup, discount application, fuel, accessorials, floor enforcement) |
| **SimulationService** | Orchestrates projection scenarios (volume, service shift, package profile, zone mix, accessorial, fuel, combined, optimize, compare) |
| **ParameterExtractionService** | Delegates NL query to AI client → returns structured `SimulationParameters` |
| **ParameterValidationService** | Validates extracted parameters against business rules; returns clarification questions if incomplete |
| **InvoiceExplainerService** | Retrieves knowledge context, calls AI, returns invoice charge explanations |
| **InvoiceKnowledgeService** | Keyword-based search across knowledge JSON files (RAG layer) |

### Repository Layer (12 interfaces)

All extend `JpaRepository` with custom query methods:

`ContractRepository` · `BaselineSnapshotRepository` · `RateCardRepository` · `ZoneMatrixRepository` · `DiscountTierRepository` · `FuelIndexRepository` · `AccessorialTypeRepository` · `DimFactorRepository` · `MinShippingChargeRepository` · `ServiceLevelRepository` · `ContractIncentiveRepository` · `PricingProgramRepository`

### Exception Handling

| Exception | HTTP Status |
|---|---|
| `RateNotFoundException` | 404 |
| `ContractNotFoundException` | 404 |
| `InvalidInputException` | 400 |
| `MethodArgumentNotValidException` | 400 |
| Generic `Exception` | 500 |

All errors return JSON with `timestamp`, `status`, `error`, and optional `fieldErrors`.

---

## Rate Calculation Formula

The 9-step deterministic calculation pipeline:

```
1. Zone         = zone_matrix lookup (origin prefix → dest prefix)
2. Billed Weight = max(actual_weight, L × W × H / dim_divisor)
3. Base Rate     = rate_card lookup (service, zone, weight band, effective date)
4. Discount %    = discount_tier lookup (program, category, weekly volume)
5. Net Transport = base_rate × (1 − discount_pct)
6. Floor Check   = max(net, floor_rate × (1 − discount) × (1 − addl_incentive_pct))
7. Fuel Charge   = net_transport × fuel_pct / 100
8. Accessorials  = Σ (fee × (1 − contract_reduction)) for triggered surcharges
9. Total         = net_transport + fuel + accessorials
```

**DIM Divisors:** Ground/Ground Residential/Three-Day = 139 | Express/Two-Day/Express Saver/International = 166

---

## Simulation Scenarios

| Scenario | Example Query | What It Computes |
|---|---|---|
| **Volume Change** | "What if I ship 35/week instead of 20?" | New tier lookup, projected annual cost delta |
| **Service Shift** | "Move 30% from GROUND to EXPRESS" | Cost impact of shifting between service categories |
| **Package Profile** | "Average weight increases to 12 lbs" | Weight ratio impact on transport costs |
| **Zone Mix** | "40% of shipments go to zone 8" | Zone-weighted rate factor changes |
| **Accessorial** | "Eliminate residential surcharges" | Surcharge exposure changes |
| **Fuel Change** | "Fuel surcharge goes to 18%" | Alternative fuel % impact |
| **Combined** | Multiple changes at once | Sequential application: volume → weight → fuel |
| **Optimize** | "How many more packages to hit next tier?" | Next tier threshold and potential savings |
| **Compare** | Side-by-side scenarios | Parallel sub-scenario evaluation |

---

## AI Integration

### Two Implementations

| Client | Activation | Use Case |
|---|---|---|
| **LocalRuleBasedAiClient** | `@Primary` (dev profile) | Regex-based parameter extraction. No API key needed. |
| **GeminiAiClient** | `@ConditionalOnProperty("gemini.api-key")` | Real NL understanding via Gemini 2.0 Flash. Temperature: 0.1. |

Both implement the `AiClient` interface:

```java
String generate(String systemPrompt, String userMessage);
```

The system prompt instructs the AI to extract structured JSON parameters from natural language queries, mapping human-readable names to exact database codes (GROUND, EXPRESS, RESIDENTIAL, etc.).

---

## Knowledge Base

Three JSON files power the invoice explanation and RAG features:

| File | Content |
|---|---|
| `surcharge-rules.json` | 8 surcharge types with codes, trigger rules, current rates, and business reasons (e.g., FUEL 14.75%, RES $5.25/pkg, AH $15/pkg) |
| `charge-explanations.json` | Dictionary explaining invoice line items (Transportation Charge, Fuel Surcharge, Incentive Credit, etc.) |
| `fuel-schedule.json` | Weekly fuel index data with diesel index, ground/air fuel percentages (8 weeks of trend data) |

---

## Testing

- **Framework:** JUnit 5 + Spring Boot Test
- **Profile:** `application-test.yml` (H2 with `create-drop`)
- **Current coverage:** Context load test

```bash
mvn test
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| AI never computes money | Auditable, deterministic; prevents hallucinated numbers |
| Results are projections, not quotes | Legal compliance; includes `confidence` and `caveat` fields |
| Discount on transport only | Per contract terms; 7 surcharges get negotiated discounts, others at full fee |
| Minimum shipping charge floor | Prevents net rate from falling below published minimums |
| Baseline from 12-month snapshot | Grounds every projection in real customer history |
| 5 service categories (not individual codes) | Reduces complexity while maintaining accuracy |
| `ddl-auto: none` | Schema managed via SQL scripts, not Hibernate auto-generation |
| WebFlux for Gemini calls | Async non-blocking HTTP; scales better under load |
| Two AiClient implementations | Local rules for dev (no API key), Gemini for production |
| USD only, single account | Hackathon scope; multi-currency deferred |

---

## Related Documentation

| Document | Description |
|---|---|
| `README-DESIGN.md` | Architecture, flow diagrams, design principles, branch strategy |
| `INPUT-PARAMS-REQUEST.md` | Full API reference with request/response shapes and examples |
| `Requirements.md` | Invoice samples and contract reference examples |
| `database/README-DDL.md` | Schema documentation |
| `database/README-DML.md` | Seed data documentation |
