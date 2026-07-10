# Billing Simulation Agent

A self-service tool that lets a customer ask, in plain language,
*"What happens to my invoice if I change my shipping behavior?"*
and get an instant, explained projection — instead of waiting 1–2 days
for an account manager to model it in a spreadsheet.

> **Docs:** technical design → [`README-DESIGN.md`](README-DESIGN.md) ·
> database → [`database/README-DDL.md`](database/README-DDL.md),
> [`database/README-DML.md`](database/README-DML.md)

---

## What it does

- Customer logs in and types a question (e.g. *"What if I move 30% of Express to Ground?"*).
- The system figures out what they mean, runs the numbers, and answers in simple business language.
- Results are shown as **projections**, not final quotes (with clear caveats).
- Every projection is grounded in the customer's **own last 12 months** of shipping history.

---

## How a question is handled

1. **Understand** – Is the question complete? If not, ask a clarifying question.
2. **Route** – Is it a *factual* question (answered from documents) or a *calculation* question?
3. **Calculate** – The Java rate engine does all the math (never the AI).
4. **Explain** – AI turns the numbers into a plain-language answer.

> Rule: **AI only reads, asks, and explains. All money math is done by the deterministic rate engine.**

---

## Pricing programs

There are **two ways** a customer's discount can work:

| Program | How the discount works | Example (Ground) |
|---|---|---|
| **Flat** ("Everyday Savings") | Fixed discount, no matter the volume | Always 39% off |
| **Volume-Tiered** ("Save as You Grow") | Discount **grows as weekly volume grows** | 36% → 44% → 52% off |

### The program we are using: **Volume-Tiered**

**Why:** the main customer question is *"what happens if I change my volume?"*
- With a flat program, more volume is just a boring straight multiply.
- With a tiered program, more volume can **unlock a better discount tier**, so the
  cost *per package* actually drops.

This produces the "aha" insight the tool is meant to deliver, e.g.:
> *"Growing to 30 shipments/week moves you into a better tier — your Ground
> discount rises from 36% to 44%, lowering your cost per package."*

The flat program is still kept in the data (as a simple one-tier case) so we can
show a comparison if needed.

---

## Services supported

| Service | Type | Discount category |
|---|---|---|
| Ground | Domestic | Ground |
| Ground Residential | Domestic | Ground |
| 3 Day Select | Domestic | Ground |
| 2 Day Air | Domestic Air | Air |
| Express Saver | Domestic Air | Air |
| Next Day Express | Domestic Air | Air |
| International Express Export | International | Intl Express Export |
| International Express Import | International | Intl Express Import |
| International Standard | International | Intl Standard |

---

## Types of questions the simulator answers

- **Volume change** – "increase / reduce my volume by X%"
- **Service shift** – "move X% of Express shipments to Ground"
- **Package profile** – "if my average package weight changes"
- **Zone mix** – "if more shipments go to farther zones"
- **Accessorial** – "if I reduce residential deliveries"

> Surcharge discounts (40% off) apply to **7 specific charges**: domestic residential,
> domestic delivery area (standard + extended), international residential,
> import delivery area (standard + extended), and export delivery area extended.
> All other surcharges (demand, additional handling, etc.) are charged in full.
- **Fuel change** – "if fuel goes up 5% next month"
- **Optimize** – "how do I reduce my cost?"
- **Explain my invoice** – "why am I charged a Demand Surcharge?"

---

## Two cost views (important)

The tool always compares **two numbers**:

1. **Published vs Net** – the list price vs your discounted price (difference = *incentive credit*, shown on the invoice).
2. **Baseline vs Projected** – your current invoice vs the what-if scenario (difference = *projected savings*).

---

## How the cost is calculated (simple view)

```
Base rate (by service, zone, weight)
  − program discount (applies to base transportation only)
  = net transportation   (never below the minimum shipping charge)
  + fuel surcharge        (fuel index based)
  + accessorial charges   (residential, delivery-area, etc.)
  − incentive credits
  = projected invoice total
```

---

## Key principles

- **Reuse, don't rewrite** – use the existing rate engine, just inject scenario parameters.
- **Projections, not quotes** – always framed with confidence / caveats.
- **Grounded in real history** – uses the customer's own 12-month baseline.
- **Ask, don't assume** – clarify ambiguous questions instead of guessing.
- **Auditable** – every scenario stores its inputs, assumptions, and outputs.
- **Secure** – results reveal sensitive pricing, so access is controlled per customer.

---

## Tech stack (planned)

- **Frontend:** React (login/signup + chat experience)
- **Backend:** Java / Spring Boot (rate engine + APIs)
- **Database:** Supabase (PostgreSQL) — shipments, rate cards, contracts
- **AI / Cloud:** Google Cloud – Vertex AI (Gemini) for language + RAG for factual answers

> Note: all pricing names and values in this project are **generic samples** for the hackathon.
