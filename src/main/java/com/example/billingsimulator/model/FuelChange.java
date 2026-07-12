package com.example.billingsimulator.model;

/**
 * Represents a hypothetical change in fuel surcharge conditions.
 * UPS fuel surcharge is recalculated weekly based on the national diesel price index.
 *
 * Examples:
 *   "What if diesel increases 10%?"         → { fuelPriceChangePct: 10.0 }
 *   "What if fuel surcharge goes to 16%?"   → { targetFuelSurchargePct: 16.0 }
 *   "What if diesel drops to $4.50/gallon?" → { targetDieselPrice: 4.50 }
 */
public class FuelChange {

    private Double fuelPriceChangePct;       // Relative change in diesel price (e.g., +10 means 10% increase)
    private Double targetFuelSurchargePct;   // Absolute target fuel surcharge percentage (e.g., 16.0)
    private Double targetDieselPrice;        // Absolute diesel price per gallon (e.g., 4.50)

    public FuelChange() {}

    public Double getFuelPriceChangePct() { return fuelPriceChangePct; }
    public void setFuelPriceChangePct(Double fuelPriceChangePct) { this.fuelPriceChangePct = fuelPriceChangePct; }

    public Double getTargetFuelSurchargePct() { return targetFuelSurchargePct; }
    public void setTargetFuelSurchargePct(Double targetFuelSurchargePct) { this.targetFuelSurchargePct = targetFuelSurchargePct; }

    public Double getTargetDieselPrice() { return targetDieselPrice; }
    public void setTargetDieselPrice(Double targetDieselPrice) { this.targetDieselPrice = targetDieselPrice; }
}
