# Shipping Invoice Dataset (Sanitized)

> Sanitized invoice derived from a carrier invoice. Company names, account numbers, invoice numbers, tracking numbers, addresses, contact names, banking details, email addresses, phone numbers, and other identifying information have been removed.

## Invoice Summary

- Invoice Date: February 7, 2026
- Country: United States
- Currency: USD
- Invoice Type: Delivery Service Invoice
- Billing Model: Weekly Payment Plan
- Total Pages: 18

## Financial Summary

| Metric | Amount (USD) |
|----------|----------:|
| Charges This Period | 1,610.35 |
| Incentive Savings | 2,140.07 |
| Previous Outstanding Balance | 1,923.95 |
| Total Outstanding Balance | 3,534.30 |
| Amount Due This Invoice | 0.00 |

## Charge Breakdown

| Category | Amount (USD) |
|----------|----------:|
| Outbound Shipments | 704.76 |
| Inbound Collect | 670.59 |
| Inbound Third Party | 102.22 |
| Returns | 35.70 |
| Adjustments & Other Charges | 49.89 |
| Service Charges | 47.19 |
| Total Charges | 1,610.35 |

## Incentive Programs Observed

- Ground Commercial Package discounts
- Ground Residential Package discounts
- Freight Pricing discounts
- Delivery Area Surcharge discounts
- Residential Surcharge discounts
- Additional Handling discounts
- Declared Value discounts
- Third-Party Billing discounts

## Shipping Services Identified

- Ground Commercial
- Ground Residential
- Ground Freight Pricing
- Next Day Air
- Next Day Air Early
- Returns Ground
- Third-Party Billing

## Common Surcharges Found

- Fuel Surcharge
- Residential Surcharge
- Delivery Area Surcharge
- Extended Delivery Area Surcharge
- Additional Handling (Weight)
- Declared Value Charges
- Third-Party Billing Fees
- Payment/Service Fees

## Geographic Information Retained

To preserve useful logistics intelligence while removing customer data:

- Country: United States
- Domestic shipment activity across multiple U.S. states
- Shipping zones observed: 2, 3, 4, 5, 6, 7, 27, 104, 107

## Example Shipment Attributes

The invoice demonstrates that shipment-level records can include:

- Service Type
- Zone
- Package Weight
- Published Charge
- Incentive Credit
- Billed Charge
- Fuel Surcharge
- Delivery Area Surcharge
- Declared Value Charges
- Additional Handling Fees

## Returns Activity

| Metric | Value |
|----------|----------:|
| Return Packages | 2 |
| Return Charges | 35.70 |

## Adjustment Activity

| Adjustment Type | Amount (USD) |
|----------|----------:|
| Weekly Printer Service Fee | 9.99 |
| Shipping Charge Correction Audit Fee | 8.25 |

## Invoice Messaging Codes

| Code | Meaning |
|------|---------|
| e | Minimum Net Shipment Charge Applied |
| ag | Minimum Rates Applied |
| bf | Custom Dimensional Weight Applied |
| PD | Dimensions Converted to Preferred Unit of Measure |
| KD | Charges Based on Customer-Provided Information |
| w | Dimensional Weight Adjustment Based Upon Audit |

## Recommended Fields for RAG / Vector Search

```text
invoice_date
country
currency
service_type
shipment_type
zone
package_weight
published_charge
incentive_credit
billed_charge
fuel_surcharge
delivery_area_surcharge
declared_value
handling_fee
returns_charge
adjustment_fee
service_charge
```
