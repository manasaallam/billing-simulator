# Database — DDL (Schema)

This folder documents the **structure** of the Billing Simulation Agent database.

- Script: [`ddl/01_schema.sql`](ddl/01_schema.sql)
- Engine: **Supabase (PostgreSQL)**
- These files are **standalone resources**. They are **not** wired into the
  Spring Boot app (they live outside `src/`), so running the app will not execute them.

## How to run (Supabase)

**Option A — Supabase SQL Editor (easiest):**
1. Open your project → **SQL Editor** → **New query**.
2. Paste the contents of `ddl/01_schema.sql` and click **Run**.

**Option B — psql via the connection string** (Project → *Settings → Database*):

```bash
psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres" -f ddl/01_schema.sql
```

Run this **first**, then load sample data with the DML script (see the DML README).

> `gen_random_uuid()` is available by default on Supabase (Postgres 15), so the
> `CREATE EXTENSION pgcrypto` line is harmless and can be left in.

## What the schema covers

| Group | Tables | Purpose |
|---|---|---|
| Identity | `account`, `app_user` | login / customer scope |
| Rate reference | `service_level`, `zone_matrix`, `rate_card`, `dim_factor`, `min_shipping_charge` | published rates + rules the engine reads |
| Fuel | `fuel_program`, `fuel_index` | weekly fuel-index based surcharge |
| Accessorials | `accessorial_type` | surcharge rule cards (residential, delivery area, …) |
| Pricing | `discount_category`, `pricing_program`, `discount_tier` | 5 service categories + **FLAT vs VOLUME_TIERED** discounts |
| Contract | `contract`, `contract_incentive` | consumed pricing (read-only), surcharge reductions, late payment fee |
| Payment / profile | `payment_plan`, `payment_plan_account`, `customer_profile` | billing plans + invoice preferences |
| Baseline | `shipment`, `shipment_charge`, `baseline_snapshot` | 12 months history + cached aggregates |
| Chat / audit | `conversation`, `chat_message`, `simulation_scenario` | conversation + auditable scenarios |
| Invoice | `invoice`, `invoice_line` | actual + projected invoices |
| Knowledge | `knowledge_article` | RAG metadata for the invoice explainer |

## Key design points

- **Five discount categories** (not individual service codes) drive pricing:
  `AIR`, `GROUND`, `INTL_EXPRESS_EXPORT`, `INTL_EXPRESS_IMPORT`, `INTL_STANDARD`.
  The `discount_category` table defines these; each `service_level` row maps to one category
  (e.g. EXPRESS + EXPRESS_SAVER + TWO_DAY all map to `AIR`). `discount_tier` references the
  category, not the individual service code — so one set of tier rows covers all air services.
- **Two pricing programs** are modelled by `pricing_program.type`
  (`FLAT` or `VOLUME_TIERED`). Discounts live in `discount_tier`, one row per
  `(program, category, volume band)`. A FLAT program uses a single band
  (`vol_min = 0`, `vol_max = NULL`).
- **Published vs Net** is stored explicitly (`published_charge` / `net_transport`
  on `shipment`, `published_amount` / `amount` on charge lines) so every invoice
  can show the *incentive credit*.
- **Minimum shipping charge** is a real rule → `min_shipping_charge`. Net can
  never fall below the published floor price.
- **Auditability** → `simulation_scenario` stores inputs, assumptions,
  baseline version and rate-card version for every projection.
- All values are **generic samples** for the hackathon.

## Supabase notes

- **Row Level Security (RLS):** for the *security & governance* requirement,
  enable RLS on customer-facing tables (`shipment`, `invoice`,
  `simulation_scenario`, `baseline_snapshot`, `conversation`, `chat_message`)
  and add a policy scoping rows to the signed-in account. For a single-account
  hackathon demo you can leave RLS off, but note it as the production control.
- **Auth:** Supabase ships its own `auth.users`. You can either keep the custom
  `app_user` table (simplest for the hackathon) or later map `app_user.user_id`
  to Supabase Auth user IDs.
