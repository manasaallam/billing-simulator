package com.example.billingsimulator.model;

/**
 * Represents a change in accessorial charge exposure.
 * Accessorials are extra fees beyond base transportation (DAS, Additional Handling, etc.).
 *
 * Examples:
 *   "What if we avoid DAS zones?"                → { surchargeCode: "DAS", changePct: -100.0 }
 *   "Reduce additional handling by 50%"           → { surchargeCode: "AH", changePct: -50.0 }
 *   "Stop using Saturday delivery"                → { surchargeCode: "SAT", changePct: -100.0 }
 *   "What if demand surcharge kicks in?"          → { surchargeCode: "DS", changePct: 100.0 }
 *
 * Known surcharge codes:
 *   DAS  - Delivery Area Surcharge ($7.75)
 *   AH   - Additional Handling (oversized/overweight)
 *   DS   - Demand Surcharge (peak periods)
 *   SAT  - Saturday Delivery
 *   DV   - Declared Value
 *   LPS  - Large Package Surcharge
 *   PAF  - Premier Air Fee
 */
public class AccessorialChange {

    private String surchargeCode;    // Which surcharge (DAS, AH, DS, SAT, DV, LPS, PAF)
    private String surchargeName;    // Human-readable name (for display)
    private Double changePct;        // % change in exposure (-100 = eliminate, +50 = 50% more)

    public AccessorialChange() {}

    public AccessorialChange(String surchargeCode, String surchargeName, Double changePct) {
        this.surchargeCode = surchargeCode;
        this.surchargeName = surchargeName;
        this.changePct = changePct;
    }

    public String getSurchargeCode() { return surchargeCode; }
    public void setSurchargeCode(String surchargeCode) { this.surchargeCode = surchargeCode; }

    public String getSurchargeName() { return surchargeName; }
    public void setSurchargeName(String surchargeName) { this.surchargeName = surchargeName; }

    public Double getChangePct() { return changePct; }
    public void setChangePct(Double changePct) { this.changePct = changePct; }
}
