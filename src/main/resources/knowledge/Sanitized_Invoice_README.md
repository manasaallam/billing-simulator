# Shipping Invoice Sample (Sanitized)

> Disclaimer: This document was derived from a sample delivery service invoice image. Company-specific information, account identifiers, addresses, phone numbers, website references, banking details, and other identifying information have been removed or generalized.

## Invoice Overview

**Document Type:** Delivery Service Invoice  
**Invoice Date:** July 4, 2026  
**Pages:** 1 of 5 (as indicated in the source document)

## Account Status Summary

### Weekly Payment Plan

| Description | Amount |
|------------|---------:|
| Amount Due This Period | $29.88 |
| Amount Outstanding (Prior Invoices) | $1,332.04 |
| Total Amount Outstanding | $1,361.33 |

## Summary of Charges

| Charge Type | Amount |
|------------|---------:|
| Service Charges | $29.29 |
| Payment Processing Fee | $0.59 |
| **Amount Due This Period** | **$29.88** |

## Payment Information

- Invoice amount due: **$29.88**
- Due date: Same as invoice date in the sample document.
- Late payments may be subject to additional fees or interest charges according to the applicable service agreement.

## Example Surcharges and Invoice Components

The invoice indicates that shipping invoices may contain:

- Base transportation/service charges
- Payment processing fees
- Fuel surcharges (where applicable)
- Late payment charges (where applicable)
- Other service-related adjustments

## Billing and Remittance Information

The original invoice contained:

- Account identifiers
- Customer name and address
- Remittance instructions
- Banking information
- Payment QR code

These details have been intentionally removed to preserve privacy and eliminate company-specific references.

## Useful Data for Logistics and RAG Use Cases

The following fields can be extracted from similar invoice documents:

```text
Invoice Date
Invoice Number
Account Number
Amount Due
Previous Balance
Total Outstanding Balance
Service Charges
Processing Fees
Fuel Surcharges
Country
Currency
Payment Due Date
```

## Country and Currency Information

Based on visible address and invoice formatting:

- Country: United States (sample invoice)
- Currency: USD ($)

## Key Financial Values Extracted

```json
{
  "invoice_date": "2026-07-04",
  "currency": "USD",
  "service_charges": 29.29,
  "processing_fee": 0.59,
  "amount_due": 29.88,
  "prior_balance": 1332.04,
  "total_outstanding": 1361.33,
  "country": "United States"
}
```
