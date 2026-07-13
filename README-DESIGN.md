# Billing Simulation Agent — Technical Design

> **Audience:** all developers on this project (rate engine team + AI/chat team + frontend team).
> This document is the single source of truth for how the system fits together.

---

## 1. Core Principle

> **The AI never computes money. The Java rate engine does all math.**

The AI layer (Gemini) is responsible for:
- Reading and understanding the user's question
- Asking follow-up questions if params are missing
- Extracting structured parameters from the conversation
- Phrasing the final answer in plain business language

The Java rate engine is responsible for:
- All cost calculations (base rate, discount, fuel, accessorials, floor, total)
- All simulation projections (volume change, service shift, fuel change, etc.)
- All results framed as projections — never as guaranteed quotes

---

## 2. Team Ownership & Branch Map

| Team | Branch | Responsibility |
|---|---|---|
| **Rate Engine** | `feature/billing-simulation-design` | JPA entities, repositories, rate calculation, simulation scenarios, `/api/rate/*` endpoints |
| **AI / Chat** | `feature/parameter-extraction` | Gemini integration, NL extraction, param validation, clarification loop, `/api/simulate` endpoint |
| **Frontend** | `mahesh_feature` | React UI, chat interface, result display |

### Integration point

```
User question (natural language)
        ↓
POST /api/simulate          ← AI team owns this
        ↓
ParameterExtractionService  ← Gemini/rule-based NL → structured params
        ↓
ParameterValidationService  ← complete? if not → ClarificationQuestion[]
        ↓
POST /api/rate/simulate     ← Rate Engine team owns this
        ↓
SimulationService.dispatch() ← deterministic Java calculation
        ↓
SimulationResponse           ← numbers back to AI team
        ↓
Gemini phrases the answer    ← AI team turns numbers into language
        ↓
Response to user
```

**Key rule:** The AI team calls `/api/rate/simulate` after all params are validated.
The rate engine team never talks directly to Gemini.

---

## 3. URL Ownership

| URL | Owner | Purpose |
|---|---|---|
| `POST /api/simulate` | AI team | Accepts raw NL query, runs extraction + clarification |
| `POST /api/simulate/clarify` | AI team | Re-run with user's answers to clarification questions |
| `POST /api/rate/quote` | Rate engine team | Single-package deterministic rate quote |
| `POST /api/rate/quote/batch` | Rate engine team | Batch rate quote (up to 50 packages) |
| `GET  /api/rate/card` | Rate engine team | List available rate card versions |
| `POST /api/rate/simulate` | Rate engine team | All simulation scenarios (dispatch by `scenarioType`) |
| `POST /api/rate/simulate/volume-change` | Rate engine team | Volume change scenario |
| `POST /api/rate/simulate/service-shift` | Rate engine team | Service shift scenario |
| `POST /api/rate/simulate/package-profile` | Rate engine team | Package weight/dim scenario |
| `POST /api/rate/simulate/zone-mix` | Rate engine team | Zone distribution scenario |
| `POST /api/rate/simulate/accessorial` | Rate engine team | Add/remove surcharge scenario |
| `POST /api/rate/simulate/fuel-change` | Rate engine team | Fuel % change scenario |
| `POST /api/rate/simulate/combined` | Rate engine team | Multiple changes at once |
| `POST /api/rate/simulate/optimize` | Rate engine team | Next tier threshold |
| `POST /api/rate/simulate/compare` | Rate engine team | Side-by-side named scenarios |

> Full request/response shapes → [`INPUT-PARAMS-REQUEST.md`](INPUT-PARAMS-REQUEST.md)

---

## 4. Request Flow (Sequence)

```
React UI
  │
  │  POST /api/simulate { naturalLanguageQuery, customerId }
  ▼
SimulationController (AI team)
  │
  ├─ ParameterExtractionService → Gemini → SimulationParameters
  │
  ├─ ParameterValidationService
  │     │
  │     ├─ incomplete → return ClarificationQuestion[] (UI asks follow-up)
  │     │
  │     └─ complete ──────────────────────────────────────────────┐
  │                                                               │
  │  POST /api/rate/simulate { contractId, scenarioType, ... }   │
  ▼                                                               │
SimulationController (Rate engine team) ◄──────────────────────┘
  │
  ├─ SimulationService.dispatch(scenarioType)
  │     ├─ volumeChange()      → tier lookup → projectTransportCost()
  │     ├─ serviceShift()      → per-category discount delta
  │     ├─ packageProfile()    → weight ratio on transport cost
  │     ├─ zoneMix()           → zone weight factor ratio
  │     ├─ accessorialChange() → fee × coverage × incentive reduction
  │     ├─ fuelChange()        → net transport × new fuel pct
  │     ├─ combined()          → volume → weight → fuel in sequence
  │     ├─ optimize()          → next tier threshold per category
  │     └─ compare()           → parallel dispatch of named sub-scenarios
  │
  └─ SimulationResponse { baselineAnnualCost, projectedAnnualCost, annualDelta, ... }
        │
        ▼
  AI team → Gemini phrases SimulationResponse into plain language
        │
        ▼
  React UI displays answer
```

---

## 5. Rate Engine Calculation Order

For every package, the engine executes these steps in order:

```
1. Zone           origin_zip prefix × dest_zip prefix → zone (2–8)
2. Billed weight  max(actual_weight, L×W×H / dim_divisor)
3. Base rate      rate_card lookup (service_code, zone, billed_weight, bill_date)
4. Discount       discount_tier lookup (program_id, discount_category, avg_weekly_volume)
                  net_transport = base_rate × (1 − discount_pct)
5. Min floor      net = max(net, floor_rate × (1 − discount) × (1 − addl_incentive_pct))
6. Fuel           net_transport × fuel_pct / 100  [from weekly fuel_index]
7. Accessorials   default_fee × (1 − contract_incentive reduction)
                  [7 surcharge codes get 40% off under volume-tiered program]
8. Total          net_transport + fuel + accessorials
```

### The 7 discountable surcharge codes (40% off for volume-tiered accounts)

| Code | Surcharge |
|---|---|
| `RESIDENTIAL` | Residential Surcharge |
| `DELIVERY_AREA` | Delivery Area Surcharge |
| `DELIVERY_AREA_EXT` | Delivery Area Extended |
| `RESIDENTIAL_INTL` | Residential (Import/Export) |
| `DELIVERY_AREA_IMPORT` | Import Delivery Area |
| `DELIVERY_AREA_IMPORT_EXT` | Import DAS Extended |
| `DELIVERY_AREA_EXPORT_EXT` | Export DAS Extended |

Non-discounted surcharges (full fee): `ADDL_HANDLING`, `DEMAND`, `SATURDAY`, `DECLARED_VALUE`, `PREMIUM_AIR`

---

## 6. Discount Tiers (Volume-Tiered Program)

Discount is looked up by **discount category** (not individual service code) and **avg weekly volume** from the account's baseline snapshot.

| Weekly Volume | AIR | GROUND | INTL_EXP_EXPORT | INTL_EXP_IMPORT | INTL_STANDARD |
|---|---|---|---|---|---|
| 0–10 | 60% | 36% | 55% | 47% | 27% |
| 11–30 | 66% | 44% | 65% | 53% | 32% |
| 31–40 | 68% | 46% | 70% | 54% | 33% |
| 41–50 | 72% | 50% | 71% | 55% | 34% |
| 51–70 | 74% | 51% | 72% | 56% | 35% |
| 71+ | 75% | 52% | 74% | 58% | 37% |

**Demo account baseline:** 20 shipments/week → GROUND discount = 44% (band 11–30).

---

## 7. Two Cost Views Returned

Every simulation response includes both:

| View | Fields | Purpose |
|---|---|---|
| **Published vs Net** | `publishedCharge`, `discountAmount`, `netTransport` | Shows the incentive credit on a per-package basis |
| **Baseline vs Projected** | `baselineAnnualCost`, `projectedAnnualCost`, `annualDelta`, `deltaPercent` | Shows the scenario impact on the full account |

---

## 8. Demo Account

| Field | Value |
|---|---|
| Company name | Demo Customer Inc. |
| Access key | `DEMO2026` |
| Company ID | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` |
| Contract ID | `CTR-001` |
| Pricing program | Volume-Tiered ("Save as You Grow") |
| Baseline period | June 2025 – May 2026 |
| Baseline total cost | $502,000/year |
| Avg weekly volume | 20 shipments/week (GROUND discount band: 44%) |
| Fuel program | FUEL_STANDARD |
| Payment terms | NET30 |
| Late payment fee | 1% per month on outstanding balance |

---

## 9. Database Tables (Summary)

| Group | Tables |
|---|---|
| Identity | `company`, `app_user`, `customer_profile` |
| Pricing | `pricing_program`, `discount_category`, `discount_tier` |
| Services | `service_level`, `dim_factor` |
| Rates | `rate_card`, `zone_matrix`, `min_shipping_charge` |
| Fuel | `fuel_program`, `fuel_index` |
| Surcharges | `accessorial_type` |
| Contract | `contract`, `contract_incentive`, `payment_plan`, `payment_plan_company` |
| Shipments | `shipment`, `shipment_charge` |
| Baseline | `baseline_snapshot` |
| Chat | `conversation`, `chat_message` |
| Scenarios | `simulation_scenario` |
| Invoice | `invoice`, `invoice_line` |
| RAG | `knowledge_article` (pgvector `vector(1536)` + HNSW index) |

Full DDL → [`database/ddl/01_schema.sql`](database/ddl/01_schema.sql)
Seed data → [`database/dml/02_seed_data.sql`](database/dml/02_seed_data.sql)

---

## 10. Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3.1, Spring Data JPA, Spring Security |
| Database | Supabase (PostgreSQL), pgbouncer (transaction pooler, port 6543) |
| AI | Google Cloud Vertex AI — Gemini 2.x Flash (classify/extract), Pro (final answer) |
| RAG | Vertex AI Vector Search or Supabase pgvector |
| Frontend | React |
| Build | Maven |

---

## 11. Key Design Decisions

| Decision | Rationale |
|---|---|
| AI never computes money | Prevents hallucinated numbers; all math is auditable Java |
| Results are projections, not quotes | Legal / compliance; always include `confidence` + `caveat` |
| Discount applies to base transport only | Per contract terms; surcharges (except 7 listed) are full fee |
| Minimum shipping charge floor | Prevents net going below published minimums |
| Baseline from 12-month snapshot | Grounds every projection in real customer history |
| USD only, single account | Hackathon scope — multi-currency and multi-account out of scope |
| `ddl-auto: none` | Schema managed via Supabase SQL Editor, not Hibernate auto-create |

---

## 12. Branch Integration Checklist

Before merging `feature/parameter-extraction` and `feature/billing-simulation-design` into `master`:

### AI team (`feature/parameter-extraction`) must do:

**1. Update Gemini system prompt — use exact DB codes**

Service codes (replace their human-readable names):
| Change from | Change to |
|---|---|
| `"Ground"` | `"GROUND"` |
| `"Ground Residential"` | `"GROUND_RES"` |
| `"Next Day Air"` / `"Express"` | `"EXPRESS"` (Next Day Express, 1-day air) |
| `"Express Saver"` | `"EXPRESS_SAVER"` |
| `"2nd Day Air"` | `"TWO_DAY"` |
| `"3 Day Select"` | `"THREE_DAY"` |
| `"International Standard"` | `"INTL_STANDARD"` |
| `"International Express Export"` | `"INTL_EXP_EXPORT"` |
| `"International Express Import"` | `"INTL_EXP_IMPORT"` |

Surcharge codes (replace their short codes):
| Change from | Change to |
|---|---|
| `"DAS"` | `"DELIVERY_AREA"` |
| `"AH"` | `"ADDL_HANDLING"` |
| `"DS"` | `"DEMAND"` |
| `"SAT"` | `"SATURDAY"` |
| `"DV"` | `"DECLARED_VALUE"` |
| `"PAF"` | `"PREMIUM_AIR"` |

**2. Use `contractId` not `customerId`**
When calling `/api/rate/simulate`, the field name is `contractId: "CTR-001"`.

**3. Replace `buildMockResult()` with a real call to `/api/rate/simulate`**
The mock in `SimulationController.java` returns hardcoded `$502K → $449K`.
Replace it with an HTTP call to `POST /api/rate/simulate` using the validated `SimulationParameters`,
then map the `SimulationResponse` back into `SimulationResult`.

### Rate engine team (`feature/billing-simulation-design`) must do:

**No changes required.** The URL conflict is already resolved — our endpoints live under `/api/rate/*`.

### Merge is safe when:
- [ ] AI team prompt outputs exact DB codes
- [ ] AI team replaced mock with real `/api/rate/simulate` call
- [ ] Both branches merged into `master` — no `SimulationController` file conflict (different packages/URLs)
