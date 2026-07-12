package com.example.billingsimulator.model;

/**
 * Represents a change in the residential vs commercial delivery mix.
 * UPS charges a Residential Surcharge (~$5.25/package) for home deliveries.
 *
 * Examples:
 *   "Reduce residential deliveries by 30%"            → { residentialChangePct: -30.0 }
 *   "Move 20% of residential to commercial addresses"  → { residentialChangePct: -20.0 }
 *   "All deliveries will be commercial"                → { targetResidentialPct: 0.0 }
 *   "Residential mix goes from 60% to 40%"             → { targetResidentialPct: 40.0 }
 */
public class DeliveryTypeChange {

    private Double residentialChangePct;    // Relative change (e.g., -30 means 30% fewer residential)
    private Double targetResidentialPct;    // Absolute target (e.g., 40.0 means 40% of deliveries are residential)

    public DeliveryTypeChange() {}

    public Double getResidentialChangePct() { return residentialChangePct; }
    public void setResidentialChangePct(Double residentialChangePct) { this.residentialChangePct = residentialChangePct; }

    public Double getTargetResidentialPct() { return targetResidentialPct; }
    public void setTargetResidentialPct(Double targetResidentialPct) { this.targetResidentialPct = targetResidentialPct; }
}
