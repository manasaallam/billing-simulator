# Customer Pricing Agreement (Sanitized)

> Purpose: Sanitized pricing-agreement summary for RAG, contract analytics, and pricing intelligence. Company-specific identifiers have been removed while preserving pricing, incentives, rate references, and invoice-calculation rules.

## Agreement Overview

This document describes a customer pricing agreement under which a carrier provides pickup and delivery services together with negotiated pricing incentives.

Key principle:

> The carrier will provide pickup and delivery services ("Services") together with agreed pricing incentives ("Incentives").

## Services Covered

### Domestic Air Services

- Next Day Air
- Next Day Air Saver
- 2nd Day Air
- 2nd Day Air A.M.
- 3 Day Select

### Ground Services

- Commercial Ground
- Residential Ground

### International Services

- International Express Export
- International Express Import
- International Standard

## Incentive Program

| Service Category | Incentive |
|------------------|-----------:|
| Air Services | 62.5% |
| Ground Services | 39% |
| International Express Export | 50% |
| International Express Import | 40% |
| International Standard | 30% |

## Accessorial Discounts

The agreement includes discounts on selected accessorial charges:

- 30% off Residential Surcharge
- 30% off Delivery Area Surcharge
- 30% off Extended Delivery Area Surcharge

## Customer Rates

- Incentives are applied as discounts against published daily transportation rates.
- Customer rates are derived from the carrier's published rate structure.
- Discounts apply to eligible transportation services.

## UPS Rate Guide / Service Guide References

Pricing and service definitions reference:

- Rate and Service Guide
- Tariff / Terms and Conditions of Service
- Published Daily Rates
- Service-specific pricing schedules

These documents define pricing, service eligibility, billing rules, and shipment conditions.

## Invoice Calculation Rules

### Base Transportation Charges

- Incentives apply to base transportation charges.
- Discounts are calculated from published daily rates.
- Incentives are applied according to the billing week of the shipment.

### Additional Charges

Unless explicitly stated otherwise, incentives do not apply to:

- Value-added services
- Accessorial charges
- Additional fees
- Surcharges
- Other non-transportation charges

### Ground Minimum Charge Rule

For Ground shipments, the customer pays the greater of:

1. Discounted transportation charge, or
2. Published 1 lb Zone 2 charge.

### Three-Day Service Minimum Charge Rule

For Three-Day service shipments, the customer pays the greater of:

1. Discounted transportation charge, or
2. Published 1 lb Zone 302 charge.

Additional incentive:

- 25% discount on the minimum charge.

### International Standard Rule

For International Standard shipments, the customer pays the greater of:

1. Discounted transportation charge, or
2. Published 1 lb rate for the applicable shipping zone.

## Incentive Eligibility Rules

- Incentives apply only to approved customer accounts.
- Incentives may not be resold or transferred.
- Services and incentives may be modified, suspended, or discontinued.
- Incentive programs remain subject to carrier pricing policies.

## Terms Affecting Billing

The following concepts directly impact invoice calculations:

- Published transportation rates
- Negotiated discounts
- Weekly billing application of incentives
- Minimum shipping charge rules
- Service eligibility requirements
- Accessorial charges and surcharges
- Zone-based pricing
- Weight-based pricing

## Recommended Fields for RAG Extraction

```text
service_type
customer_rate
discount_percentage
incentive_program
accessorial_discount
base_transportation_rate
published_rate
rate_guide_reference
minimum_charge_rule
service_eligibility
billing_week
zone_pricing
weight_pricing
international_service
ground_service
air_service
```

## Removed Information

- Customer names
- Account numbers
- Addresses
- Contact information
- Legal entity details
- Organization-specific identifiers
