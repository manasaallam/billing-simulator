# Database — DML (Seed / Sample Data)

This folder documents the **sample data** used to demo the Billing Simulation Agent.

- Script: [`dml/02_seed_data.sql`](dml/02_seed_data.sql)
- Engine: **Supabase (PostgreSQL)**
- Standalone resource — **not** run by the Spring Boot app. Load it manually.

## How to run (Supabase)

Run **after** the schema (`ddl/01_schema.sql`).

**Supabase SQL Editor:** paste `dml/02_seed_data.sql` into a new query and **Run**.

**psql:**

```bash
psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres" -f dml/02_seed_data.sql
```

## What gets seeded

| Data | Notes |
|---|---|
| **Discount categories** | 5 categories: AIR, GROUND, INTL_EXPRESS_EXPORT, INTL_EXPRESS_IMPORT, INTL_STANDARD |
| Service levels | GROUND, GROUND_RES, THREE_DAY, TWO_DAY, EXPRESS_SAVER, EXPRESS, INTL_EXP_EXPORT, INTL_EXP_IMPORT, INTL_STANDARD (each mapped to its category) |
| Dim factors | dimensional-weight divisors per service |
| Accessorial rule cards | 12 codes: RESIDENTIAL, DELIVERY_AREA, DELIVERY_AREA_EXT, RESIDENTIAL_INTL, DELIVERY_AREA_IMPORT, DELIVERY_AREA_IMPORT_EXT, DELIVERY_AREA_EXPORT_EXT, ADDL_HANDLING, DEMAND, SATURDAY, DECLARED_VALUE, PREMIUM_AIR |
| Fuel program + weekly index | weekly index covering Jul 2025 → Jul 2026 (fuel % ranging ~12.00 → 16.50) |
| Zone matrix | origin/dest prefix → zone; covers zones 2–8 across ATL/CHI/DAL/LA/NY origins |
| Rate card | two versions: `2026-01` (current) and `2025-01` (last-year) across more services / zones / weight breaks |
| Minimum shipping charge | floors for GROUND and THREE_DAY |
| **Pricing programs** | one FLAT + one VOLUME_TIERED |
| **Discount tiers** | all 5 categories × 6 volume bands for TIERED; all 5 categories single band for FLAT |
| Account / user / profile | one demo customer |
| Contract | demo account → **VOLUME_TIERED** program, **40% off 7 surcharge codes** (domestic + international residential/delivery-area variants) |
| Sample shipments | current-year slice (2026) **plus 30 last-year shipments (Jul–Dec 2025)** spanning zones 2–8 and services GROUND, GROUND_RES, THREE_DAY, TWO_DAY, EXPRESS_SAVER, EXPRESS; includes itemized charge lines |
| Baseline snapshot | frozen aggregate, `avg_weekly_volume = 20` |
| Knowledge articles | RAG seed for the invoice explainer |

## The pricing decision (reflected in the seed)

The demo account (`ACCT-1001`, contract `CTR-001`) runs on the
**VOLUME_TIERED** program (`Save as You Grow`).

Why it matters for the demo — discounts are per **service category**, not individual service code.
GROUND category (GROUND, GROUND_RES, THREE_DAY) discount grows with volume:

| Avg weekly volume | GROUND | AIR | INTL_EXP_EXPORT | INTL_EXP_IMPORT | INTL_STANDARD |
|---|---|---|---|---|---|
| 0–10  | 36% | 60% | 55% | 47% | 27% |
| 11–30 | 44% | 66% | 65% | 53% | 32% |
| 31–40 | 46% | 68% | 70% | 54% | 33% |
| 41–50 | 50% | 72% | 71% | 55% | 34% |
| 51–70 | 51% | 74% | 72% | 56% | 35% |
| 71+   | 52% | 75% | 74% | 58% | 37% |

The baseline sits at **20 shipments/week (44% tier)**, so a *"grow my volume"*
scenario can move the account into the 46–52% tiers and **lower the cost per
package** — the headline insight of the tool.

The **FLAT** program is also seeded (single-band 39% GROUND / 62.5% AIR) so a
fixed-discount comparison can be shown if needed.

## Notes

- `app_user.auth_provider_uid` is a placeholder — replace with a real BCrypt hash
  (local auth) or the OAuth provider UID from the app.
- `knowledge_article.embedding` is seeded as `NULL`; the app generates the
  1536-dim vector at runtime. The column is nullable in the schema for this reason.
- All names and numbers are **generic samples** for the hackathon; they do not
  represent any real carrier's published pricing.
