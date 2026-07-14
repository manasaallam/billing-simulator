# Billing Simulation API — Input Parameters Reference

All endpoints accept and return **JSON**. Base URL: `http://localhost:8080`

---

## How the AI uses this API

The AI only needs to know **two endpoints**:

| Question type | Endpoint | Request object |
|---|---|---|
| "What does it cost to ship this package?" | `POST /api/rate/quote` | `RateQuoteRequest` |
| "What if I change volume / service / weight / fuel / zones / surcharges?" | `POST /api/rate/simulate` | `SimulationRequest` with `scenarioType` |

> **URL ownership:** `POST /api/simulate` is owned by the AI/chat team (natural language → extraction → clarification flow). Once the AI has all parameters, it calls `POST /api/rate/simulate` with the structured request.

The AI's job:
1. Classify the question → **RATE_QUOTE** or **SIMULATION**
2. Extract the parameters from the question
3. Build the matching request object
4. POST to the right endpoint

For simulations, the `scenarioType` field inside the body tells the backend which calculation to run — the AI does **not** need to pick a sub-URL.

---

## Reference Data (Demo Account)

| Key | Value |
|---|---|
| `contractId` | `CTR-001` |
| `accountId` | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` |
| `baselineId` | leave blank — API resolves the latest snapshot automatically |

### Valid `serviceCode` values

| Code | Display Name | Category |
|---|---|---|
| `GROUND` | Ground | GROUND |
| `GROUND_RES` | Ground Residential | GROUND |
| `THREE_DAY` | 3 Day Select | GROUND |
| `TWO_DAY` | 2 Day Air | AIR |
| `EXPRESS_SAVER` | Express Saver | AIR |
| `EXPRESS` | Next Day Express | AIR |
| `INTL_EXP_EXPORT` | International Express Export | INTL_EXPRESS_EXPORT |
| `INTL_EXP_IMPORT` | International Express Import | INTL_EXPRESS_IMPORT |
| `INTL_STANDARD` | International Standard | INTL_STANDARD |

### Valid `accessorialCode` values

| Code | Fee | Discountable? |
|---|---|---|
| `RESIDENTIAL` | $5.25/pkg | Yes (40% off) |
| `DELIVERY_AREA` | $7.75/pkg | Yes (40% off) |
| `DELIVERY_AREA_EXT` | $9.25/pkg | Yes (40% off) |
| `RESIDENTIAL_INTL` | $5.25/pkg | Yes (40% off) |
| `DELIVERY_AREA_IMPORT` | $7.75/pkg | Yes (40% off) |
| `DELIVERY_AREA_IMPORT_EXT` | $9.25/pkg | Yes (40% off) |
| `DELIVERY_AREA_EXPORT_EXT` | $9.25/pkg | Yes (40% off) |
| `ADDL_HANDLING` | $15.00/pkg | No |
| `DEMAND` | $3.50/pkg | No |
| `SATURDAY` | $16.00/shipment | No |
| `DECLARED_VALUE` | $3.00/pkg | No |
| `PREMIUM_AIR` | $4.25/pkg | No |
| `DUTY` | actual (customs) | No |
| `VAT` | actual (customs) | No |
| `BROKERAGE_FEE` | actual (broker) | No |
| `TRAILER_PICKUP` | $2.50/shipment | No |

---

## 1. Single-Package Rate Quote

**`POST /api/rate/quote`**

### Mandatory fields

| Field | Type | Notes |
|---|---|---|
| `contractId` | string | e.g. `"CTR-001"` |
| `serviceCode` | string | See valid values above |
| `originZip` | string | 5-digit US ZIP |
| `destZip` | string | 5-digit US ZIP |
| `actualWeightLb` | decimal | Minimum `0.1` |

### Optional fields

| Field | Type | Default | Notes |
|---|---|---|---|
| `lengthIn` | decimal | — | All three dims required together for DIM weight |
| `widthIn` | decimal | — | |
| `heightIn` | decimal | — | |
| `residential` | boolean | `false` | Triggers RESIDENTIAL surcharge |
| `declaredValue` | decimal | — | Triggers DECLARED_VALUE surcharge if > $100 |
| `billWeek` | string | current week | ISO format: `"2026-W25"` |
| `packageCount` | integer | `1` | Multiplies all per-package charges |

### Example request

```json
{
  "contractId": "CTR-001",
  "serviceCode": "GROUND",
  "originZip": "30301",
  "destZip": "60601",
  "actualWeightLb": 8.5,
  "lengthIn": 12,
  "widthIn": 10,
  "heightIn": 6,
  "residential": true,
  "billWeek": "2026-W25",
  "packageCount": 1
}
```

---

## 2. Batch Rate Quote

**`POST /api/rate/quote/batch`**

Same as above but wrapped in a JSON **array**. Maximum 50 items per call.

```json
[
  {
    "contractId": "CTR-001",
    "serviceCode": "GROUND",
    "originZip": "30301",
    "destZip": "60601",
    "actualWeightLb": 5.0,
    "residential": false
  },
  {
    "contractId": "CTR-001",
    "serviceCode": "EXPRESS",
    "originZip": "30301",
    "destZip": "90001",
    "actualWeightLb": 2.0,
    "residential": true
  }
]
```

---

## 3. Rate Card Versions

**`GET /api/rate/card`**

No request body. Returns list of available rate card version strings (e.g. `["2026-01"]`).

---

## 4. Simulation Endpoints

**Base path: `/api/rate/simulate`**

All simulation endpoints share a common base. `scenarioType` is **set automatically by the endpoint URL** — do not include it in the request body.

### Common fields (all scenarios)

| Field | Type | Required? | Notes |
|---|---|---|---|
| `contractId` | string | **REQUIRED** | e.g. `"CTR-001"` |
| `baselineId` | string | Optional | UUID of `baseline_snapshot`. If omitted, latest is used. |

---

### 4a. Volume Change

**`POST /api/rate/simulate/volume-change`**

*"What if I ship X packages per week instead of my current 20?"*
Tier upgrade/downgrade impact on annual cost.

| Field | Type | Required? | Notes |
|---|---|---|---|
| `newWeeklyVolume` | integer | **REQUIRED** | Must be > 0 |

```json
{
  "contractId": "CTR-001",
  "newWeeklyVolume": 35
}
```

---

### 4b. Service Shift

**`POST /api/rate/simulate/service-shift`**

*"What if I move 30% of my GROUND volume to THREE_DAY?"*

| Field | Type | Required? | Notes |
|---|---|---|---|
| `fromService` | string | **REQUIRED** | Valid service code |
| `toService` | string | **REQUIRED** | Valid service code |
| `shiftFraction` | decimal | **REQUIRED** | `0.01` – `1.0` (e.g. `0.30` = 30%) |

```json
{
  "contractId": "CTR-001",
  "fromService": "GROUND",
  "toService": "THREE_DAY",
  "shiftFraction": 0.30
}
```

---

### 4c. Package Profile

**`POST /api/rate/simulate/package-profile`**

*"What if my average package weight increases to 15 lbs?"*

| Field | Type | Required? | Notes |
|---|---|---|---|
| `newAvgWeightLb` | decimal | **REQUIRED** | New projected average weight |
| `newAvgLengthIn` | decimal | Optional | For DIM weight impact |
| `newAvgWidthIn` | decimal | Optional | For DIM weight impact |
| `newAvgHeightIn` | decimal | Optional | For DIM weight impact |

```json
{
  "contractId": "CTR-001",
  "newAvgWeightLb": 15.0
}
```

---

### 4d. Zone Mix

**`POST /api/rate/simulate/zone-mix`**

*"What if 40% of my shipments go to zone 8 instead of 10%?"*

| Field | Type | Required? | Notes |
|---|---|---|---|
| `zoneDistribution` | object | **REQUIRED** | Map of zone (string) → fraction. Values must sum to `1.0` |

```json
{
  "contractId": "CTR-001",
  "zoneDistribution": {
    "2": 0.20,
    "5": 0.40,
    "8": 0.40
  }
}
```

---

### 4e. Accessorial Change

**`POST /api/rate/simulate/accessorial`**

*"What if all my packages stop going to residential addresses?"*
*"What if all packages need Saturday delivery?"*

At least one of `addAccessorialCodes` or `removeAccessorialCodes` must be populated.

| Field | Type | Required? | Notes |
|---|---|---|---|
| `removeAccessorialCodes` | string[] | Optional | Codes to remove from all shipments |
| `addAccessorialCodes` | string[] | Optional | Codes to apply to all shipments |

```json
{
  "contractId": "CTR-001",
  "removeAccessorialCodes": ["RESIDENTIAL"],
  "addAccessorialCodes": []
}
```

```json
{
  "contractId": "CTR-001",
  "addAccessorialCodes": ["SATURDAY"],
  "removeAccessorialCodes": []
}
```

---

### 4f. Fuel Change

**`POST /api/rate/simulate/fuel-change`**

*"What if the fuel surcharge increases to 18%?"*

| Field | Type | Required? | Notes |
|---|---|---|---|
| `hypotheticalFuelPct` | decimal | **REQUIRED** | Fuel % as a number (e.g. `18.0` = 18%) |

```json
{
  "contractId": "CTR-001",
  "hypotheticalFuelPct": 18.0
}
```

---

### 4g. Combined

**`POST /api/rate/simulate/combined`**

*"What if I ship 35/week, packages weigh 12 lbs avg, and fuel hits 18%?"*
Apply any combination of the scenario fields at once. Changes are applied in order: volume → weight → fuel.

Populate any combination of:
- `newWeeklyVolume`
- `newAvgWeightLb`
- `hypotheticalFuelPct`

```json
{
  "contractId": "CTR-001",
  "newWeeklyVolume": 35,
  "newAvgWeightLb": 12.0,
  "hypotheticalFuelPct": 18.0
}
```

---

### 4h. Optimize (Next Tier)

**`POST /api/rate/simulate/optimize`**

*"How many more packages per week do I need to ship to hit the next discount tier?"*

No extra fields required beyond `contractId`.

```json
{
  "contractId": "CTR-001"
}
```

---

### 4i. Compare

**`POST /api/rate/simulate/compare`**

*"Show me side-by-side: shipping 30/week vs 50/week vs switching to Express."*

| Field | Type | Required? | Notes |
|---|---|---|---|
| `compareScenarios` | array | **REQUIRED** | List of `{ "label": string, "request": SimulationRequest }` |

Each `request` in the array uses the same fields as the individual scenario endpoints. `contractId` and `baselineId` are inherited from the parent if not set.

```json
{
  "contractId": "CTR-001",
  "compareScenarios": [
    {
      "label": "Ship 30/week",
      "request": {
        "scenarioType": "VOLUME_CHANGE",
        "newWeeklyVolume": 30
      }
    },
    {
      "label": "Ship 50/week",
      "request": {
        "scenarioType": "VOLUME_CHANGE",
        "newWeeklyVolume": 50
      }
    },
    {
      "label": "Shift 30% to Express",
      "request": {
        "scenarioType": "SERVICE_SHIFT",
        "fromService": "GROUND",
        "toService": "EXPRESS",
        "shiftFraction": 0.30
      }
    }
  ]
}
```

---

## Response Shape (Simulation)

Every simulation response includes:

| Field | Description |
|---|---|
| `baselineAnnualCost` | Current annual cost from baseline snapshot |
| `baselineWeeklyCost` | baselineAnnualCost / 52 |
| `projectedAnnualCost` | Estimated annual cost after the change |
| `projectedWeeklyCost` | projectedAnnualCost / 52 |
| `annualDelta` | projectedAnnualCost − baselineAnnualCost (negative = savings) |
| `deltaPercent` | % change |
| `confidence` | `HIGH` / `MEDIUM` / `LOW` |
| `caveat` | Plain-English note about projection assumptions |
| Scenario-specific fields | e.g. `tierUpgradeNote`, `optimizationHints`, `compareEntries` |

---

## Error Responses

| HTTP | Meaning |
|---|---|
| `400` | Validation failed — missing required field or invalid value. `message` field explains which field. |
| `404` | Contract, baseline, rate card, zone, or fuel index not found for given inputs. |
| `500` | Unexpected server error. |

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "newWeeklyVolume must be a positive integer"
}
```
