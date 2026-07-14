# API Curl Reference

**App:** `java -jar target/billing-simulator-0.0.1-SNAPSHOT.jar`  
**Base:** `http://localhost:8080`

> **After login, save your token:**
> ```
> TOKEN=<paste token from login response>
> ```
> All non-auth endpoints require: `-H "Authorization: Bearer $TOKEN"`

---

## Auth

**Register new user**
```
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo User","accessKey":"DEMO2026","email":"demo@customer.com","password":"Demo@1234","confirmPassword":"Demo@1234"}'
```

**Login**
```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@customer.com","password":"Demo@1234"}'
```

**Duplicate email (expect 409)**
```
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"X","accessKey":"DEMO2026","email":"demo@customer.com","password":"Demo@1234","confirmPassword":"Demo@1234"}'
```

**Wrong password (expect 401)**
```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@customer.com","password":"wrong"}'
```

**Bad access key (expect 404)**
```
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"X","accessKey":"BADKEY","email":"new@test.com","password":"Demo@1234","confirmPassword":"Demo@1234"}'
```

---

## Rate Quote

**Single package — Ground zone 5 commercial**
```
curl -X POST http://localhost:8080/api/rate/quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"serviceCode":"GROUND","originZip":"30301","destZip":"60601","actualWeightLb":8.0}'
```

**Single package — Ground residential (surcharge applied)**
```
curl -X POST http://localhost:8080/api/rate/quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"serviceCode":"GROUND","originZip":"30301","destZip":"60601","actualWeightLb":8.0,"residential":true}'
```

**Single package — Express with dim weight (18x14x12 / 166)**
```
curl -X POST http://localhost:8080/api/rate/quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"serviceCode":"EXPRESS","originZip":"30301","destZip":"60601","actualWeightLb":5.0,"lengthIn":18,"widthIn":14,"heightIn":12}'
```

**Single package — Ground zone 8 long haul with declared value**
```
curl -X POST http://localhost:8080/api/rate/quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"serviceCode":"GROUND","originZip":"30301","destZip":"90210","actualWeightLb":15.0,"declaredValue":250.00}'
```

**Single package — Three Day Select**
```
curl -X POST http://localhost:8080/api/rate/quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"serviceCode":"THREE_DAY","originZip":"30301","destZip":"60601","actualWeightLb":10.0}'
```

**Batch — 3 packages in one call**
```
curl -X POST http://localhost:8080/api/rate/quote/batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '[{"serviceCode":"GROUND","originZip":"30301","destZip":"60601","actualWeightLb":8.0},{"serviceCode":"EXPRESS","originZip":"30301","destZip":"60601","actualWeightLb":5.0},{"serviceCode":"THREE_DAY","originZip":"30301","destZip":"75201","actualWeightLb":12.0,"residential":true}]'
```

**Rate card versions**
```
curl http://localhost:8080/api/rate/card
```

---

## Rate Simulation

**Volume change — grow 20 → 50 pkgs/wk (crosses discount tier)**
```
curl -X POST http://localhost:8080/api/rate/simulate/volume-change \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"newWeeklyVolume":50}'
```

**Service shift — move 30% of Ground to Express**
```
curl -X POST http://localhost:8080/api/rate/simulate/service-shift \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fromService":"GROUND","toService":"EXPRESS","shiftFraction":0.30}'
```

**Package profile — heavier and larger boxes**
```
curl -X POST http://localhost:8080/api/rate/simulate/package-profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"newAvgWeightLb":14.0,"newAvgLengthIn":16,"newAvgWidthIn":12,"newAvgHeightIn":10}'
```

**Zone mix — redistribute to shorter haul zones**
```
curl -X POST http://localhost:8080/api/rate/simulate/zone-mix \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"zoneDistribution":{"2":0.40,"3":0.20,"5":0.30,"8":0.10}}'
```

**Accessorial — add peak Demand surcharge, drop Residential**
```
curl -X POST http://localhost:8080/api/rate/simulate/accessorial \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"addAccessorialCodes":["DEMAND"],"removeAccessorialCodes":["RESIDENTIAL"]}'
```

**Fuel change — fuel drops from 15% to 10%**
```
curl -X POST http://localhost:8080/api/rate/simulate/fuel-change \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"hypotheticalFuelPct":10.0}'
```

**Combined — volume up + service shift + fuel change together**
```
curl -X POST http://localhost:8080/api/rate/simulate/combined \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"newWeeklyVolume":55,"fromService":"GROUND","toService":"TWO_DAY","shiftFraction":0.20,"hypotheticalFuelPct":12.0}'
```

**Optimize — find the cheapest service mix**
```
curl -X POST http://localhost:8080/api/rate/simulate/optimize \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```

**Compare — 4 scenarios side by side**
```
curl -X POST http://localhost:8080/api/rate/simulate/compare \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"compareScenarios":[{"label":"Vol 30/wk","request":{"scenarioType":"VOLUME_CHANGE","newWeeklyVolume":30}},{"label":"Vol 50/wk","request":{"scenarioType":"VOLUME_CHANGE","newWeeklyVolume":50}},{"label":"Vol 71+/wk","request":{"scenarioType":"VOLUME_CHANGE","newWeeklyVolume":75}},{"label":"Fuel 10%","request":{"scenarioType":"FUEL_CHANGE","hypotheticalFuelPct":10.0}}]}'
```

**Dispatch — single endpoint, scenarioType in body**
```
curl -X POST http://localhost:8080/api/rate/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"scenarioType":"VOLUME_CHANGE","newWeeklyVolume":40}'
```

---

## NL Simulation (AI)

**Volume question**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"What if I increase my volume by 20 percent?"}'
```

**Service shift question**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"What would happen if I moved 25% of my ground shipments to express?"}'
```

**Fuel question**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"How much would I save if fuel surcharge drops to 10 percent?"}'
```

**Multi-lever question**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"What if my volume grows to 50 packages and I shift 20% from ground to two day air?"}'
```

**Vague question (returns clarification prompts)**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"How can I save money?"}'
```

**Clarification reply (skip AI re-extraction)**
```
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"naturalLanguageQuery":"Grow volume to 50 packages per week","extractedParameters":{"scenarioType":"VOLUME_CHANGE","newWeeklyVolume":50}}'
```

---

## Invoice Explainer

**Fuel surcharge**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"Why am I being charged a fuel surcharge and how is it calculated?"}'
```

**Residential surcharge**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"Why is there a residential surcharge on my invoice?"}'
```

**Delivery area surcharge**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"What is the Delivery Area Surcharge and why was it applied?"}'
```

**Discount tier**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"How is my discount calculated and which tier am I in?"}'
```

**Minimum charge floor**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"Why was I charged more than my discounted rate on a light package?"}'
```

**Specific shipment**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"Why was shipment 1A0001 charged $19.00?","trackingNumber":"1A0001"}'
```

**Demand surcharge**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"What is a Demand Surcharge and when is it applied?"}'
```

**Declared value fee**
```
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","question":"Why am I charged extra for declared value on packages over $100?"}'
```

---

## Health

**Health check**
```
curl http://localhost:8080/actuator/health
```
