# Shipping Invoice Knowledge Base Entry (Sanitized)

> Purpose: Sanitized logistics invoice record suitable for RAG, vector databases, analytics, and billing simulations.

## Billing Summary

- Invoice Date: March 11, 2028
- Invoice Due Date: March 11, 2028
- Billing Cycle: Weekly Payment Plan
- Country: United States
- Currency: USD

## Financial Summary

| Metric | Amount (USD) |
|----------|----------:|
| Amount Due This Period | 1,631.00 |
| Outstanding Balance (Prior Invoices) | 538.77 |
| Total Outstanding Balance | 2,169.77 |
| Outbound Charges | 1,544.49 |
| Service Charges | 54.53 |
| Payment Processing Fee | 31.98 |

## Service Types Observed

- Ground Commercial
- Ground Residential
- Next Day Air Commercial
- Next Day Air Saver Commercial
- Next Day Air Saver Residential
- Internet Shipping

## Accessorial Charges Observed

- Residential Surcharge
- Delivery Area Surcharge
- Demand Surcharge (Residential)
- Demand Surcharge (Commercial)
- Fuel Surcharge
- Premier Air Fee
- Trailer Pickup Adjustment
- Print Invoice Fee
- Weekly Service Charge

## Shipment Characteristics

- Residential deliveries
- Commercial deliveries
- Air shipments
- Ground shipments
- Weight-based pricing
- Zone-based pricing
- Domestic shipments

## Geographic Metadata

- Country: United States
- Shipping Zones Observed: 4, 6, 8, 108, 133, 134, 138

## Payment Terms

- Payment due on invoice due date.
- Late payments may incur a 9.9% fee.
- Weekly billing cycle.

## Service Charge Breakdown

| Charge Type | Amount (USD) |
|-------------|-------------:|
| Print Invoice Fee | 5.00 |
| Weekly Service Charge | 39.00 |
| Fuel Surcharge | 10.53 |
| Total Service Charges | 54.53 |

## Historical Balance Information

The invoice references multiple prior weekly invoices that contribute to the outstanding balance of 538.77 USD.

## Recommended Fields for Vectorization

```text
invoice_date
invoice_due_date
country
currency
billing_cycle
service_type
shipment_type
zone
package_weight
amount_due
outstanding_balance
service_charge
fuel_surcharge
residential_surcharge
delivery_area_surcharge
payment_processing_fee
late_payment_fee
```

## Removed Information

The following information was intentionally removed:

- Company names
- Customer names
- Account numbers
- Invoice numbers
- Tracking numbers
- User IDs
- Sender and receiver names
- Street addresses
- ZIP codes
- Banking information
- Tax identifiers
- Email addresses
- Phone numbers
- ACH details
- Payment URLs

This preserves logistics and billing knowledge while removing customer-specific identifiers.
