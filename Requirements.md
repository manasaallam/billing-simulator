For Your Hackathon (Billing Simulator / Invoice Explainer) 

The most valuable contract attributes to extract from a contract PDF would be: 

Category 

Examples 

Account Info 

Customer Number, Account Number 

Contract Dates 

Start Date, End Date 

Discounts 

Ground Discount %, Air Discount % 

Incentives 

Revenue commitment rebates 

Accessorial Terms 

DAS, Fuel, Residential 

Billing Cycle 

Weekly, Monthly, Semi-Monthly 

Payment Terms 

Net 15, Net 30 

Rate Type 

Published vs Negotiated 

Service Eligibility 

Ground, Air, International 

Minimum Commitments 

Revenue or Volume commitments 

 

(MY) Q0631379MY-01 GD EXPRESS SDN BHD_ 

This is particularly interesting because it explicitly references: 

Shipment objectives 

Committed shipping volume 

Discounts 

Updated rate charts 

Incentives tied to shipping behavior 

"The discounts and terms detailed in this agreement are based on the Customer's committed level of shipping activity..." 

"Updated rate charts will be made available to Customer..." 

For a billing simulator, this is gold because it links: 

Volume Commitment  

       ↓  

Rate Chart  

         ↓  

Discount  

       ↓  

Invoice Amount 

 

 

3. 104-UPS Small Package LOA_LX_LongSysLegal_EXE_Liang Zhixiong 

Contains: 

Customer rates 

Discounts 

Incentive programs 

Accessorial fees 

UPS Rate Guide references 

"Incentives may be in the form of discounts, discounted rates, waivers..."  

"Customer will pay applicable surcharges and accessorial fees..." 

4. Everyday Savings 2026 

A customer pricing agreement. 

Contains: 

UPS services 

Incentives 

Rate guide references 

Terms controlling invoice calculations 

"UPS will provide the pickup and delivery services ('Services') with the Incentives set forth above." 

5. Save As You Grow 2026 

Looks like a customer incentive contract. 

Contains: 

Base transportation discounts 

Surcharge handling 

Billing timing 

Customer payment account references 

"Incentives represent a discount from the Daily Rates..." [Save As Yo...Grow 2026 | PDF] 

"Incentives will be applied based on the week the package is billed... 

Even More Valuable Than Contracts 

I also found actual rate-card documents containing pricing tables: 

EC 60ZB (No Final Mile) Weekly Rate Card eff 6.22.26....pdf [EC 60ZB (N...8T06-48-31 | PDF] 

CA 60ZB (No Final Mile) Weekly Rate Card eff 4.6.26....pdf [CA 60ZB (N...3T05-46-02 | PDF] 

Multiple weekly rate-card versions with actual origin-destination prices and weight breaks. [EC 60ZB (N...8T06-48-31 | PDF], [CA 60ZB (N...3T05-46-02 | PDF], [CA 60ZB (N...7T05-58-14 | PDF] 

These include real-looking tables such as: 

Origin 

Destination 

100kg 

300kg 

500kg 

CAN 

JFK 

Rate 

Rate 

Rate 

SHA 

ORD 

Rate 

Rate 

Rate 

For Your Hackathon 

If your goal is Billing Simulation + Smart Invoice Explainer, I'd prioritize: 

JFPB_ASO Agreement (Signed 2.5.2025) — actual signed agreement. [JFPB_ASO A...2.5.2025) | PDF] 

(MY) Q0631379MY-01 GD EXPRESS SDN BHD_ — volume commitments + rate charts. [(MY) Q0631...S SDN BHD_ | PDF] 

104-UPS Small Package LOA_LX_LongSysLegal_EXE_Liang Zhixiong — discounts/accessorials. [104-UPS Sm...g Zhixiong | PDF] 

Weekly Rate Card PDFs — actual pricing tables. [EC 60ZB (N...8T06-48-31 | PDF], [CA 60ZB (N...3T05-46-02 | PDF] 

Those four together are much closer to the inputs FCB/FBR would ultimately need for bill generation than the DAP sample contract. 

1. Sample_Invoice_A ⭐ Best Domestic Invoice Sample 

This is an actual delivery service invoice with line-level billing components: 

Contains: 

Tracking Number 

Service Type 

Zone 

Weight 

Published Charge 

Incentive Credit 

Billed Charge 

Fuel Surcharge 

Delivery Area Surcharge (DAS) 

Declared Value Charge 

Example: 

Published Charge = 20.87 

Incentive Credit = -6.55 

Billed Charge = 14.32 

 

Delivery Area Surcharge = 5.70 

Fuel Surcharge = 5.51 

Exactly the kind of charge explanation your AI could break down. 

2. 2502_003_SHD_Invoice_TC01 ⭐ Best for Charge Justification 

Contains shipment-level details: 

Ground Residential 

Next Day Air Saver 

Residential Surcharge 

Demand Surcharge 

Fuel Surcharge 

Premier Air Fee 

Tracking Number level charges 

Example: 

Ground Residential = 19.36 

Residential Surcharge = 6.50 

Demand Surcharge = 0.25 

Fuel Surcharge = 7.18 

This is almost perfect for an Invoice Explainer demo. 

3. Fuel surcharge Impact Surcharge Code ⭐ Best for Accessorial Analysis 

Large invoice with multiple surcharge types: 

Contains: 

Fuel Surcharge 

Duty & Tax Forwarding 

International Processing Fee 

Delivery Area Surcharge 

Saturday Delivery 

UPS Access Point Hold Service 

Example: 

Fuel Surcharge = 1077.34 

Delivery Area Surcharge = 39.30 

International Processing Fee = 20.00 

Excellent for showing: 

"Why is fuel surcharge higher than expected? 

4. Cycle1-6426YY Invoice 

This is the invoice summary page customers see. 

Contains: 

Amount Due 

Outstanding Balance 

Shipping API Charges 

Service Charges 

Payment Processing Fee 

Example: 

Shipping API = 127.11 

Service Charges = 5.00 

Payment Processing Fee = 2.64 

Useful if you're modeling Billing Center dashboards. 

5. TC1_TC2_TC3_Invoice 

Contains: 

Invoice Summary 

ACH Remittance Instructions 

Consolidated Billing Information 

Outstanding Invoice Totals 

Looks like a consolidated account invoice. 

6. Sample Invoice Domestic 

Contains: 

Weekly invoice history 

Outstanding balance history 

Multiple invoice numbers 

Useful for Billing Center account views 

7. Sample Invoice Domestic - Billing Center Screen Shot 

Appears to be an actual Billing Center UI screenshot PDF. The search result doesn't expose contents, but it's likely worth opening if you're looking for the customer-facing Billing Center presentation layer. 

What I would use for the Hackathon 

If I were building your Smart Invoice Explainer, I'd use data from these three: 

Sample_Invoice_A → invoice structure [Sample_Invoice_A | PDF] 

2502_003_SHD_Invoice_TC01 → surcharge explanations [2502_003_S...voice_TC01 | PDF] 

Fuel surcharge Impact Surcharge Code → advanced accessorials 

From those invoices you can derive a knowledge graph like: 

Invoice 

├── Shipment 

│    ├── Service Type 

│    ├── Weight 

│    ├── Zone 

│ 

├── Freight Charge 

├── Fuel Surcharge 

├── Residential Surcharge 

├── DAS 

├── Saturday Delivery 

├── Processing Fee 

├── Tax/Duty Charges 

│ 

└── Total Amount Due 

That would give your RAG/Agent enough context to answer: 

Why was I charged this amount? 

Why is fuel surcharge so high? 

Which contract discount was applied? 

What-if fuel surcharge was 5% lower? 

What-if shipment weight changed from 12 lb to 8 lb? 

These are probably the closest invoice samples I found to actual UPS Billing Center output. 

 

 

 