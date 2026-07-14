package com.example.billingsimulator.model;

/**
 * Represents a change in package characteristics (weight, dimensions).
 * UPS uses dimensional weight pricing — larger/heavier packages cost more.
 *
 * Examples:
 *   "Weight increases from 2lb to 5lb"   → { averageWeight: 5.0, weightUnit: "LB" }
 *   "Switch to larger boxes (20x15x12)"  → { averageLength: 20, averageWidth: 15, averageHeight: 12, dimensionUnit: "IN" }
 *   "Package weight doubles"             → handled as VolumeChange with weight context
 */
public class PackageProfile {

    private Double averageWeight;     // Target average weight per package
    private String weightUnit;        // LB or KG
    private Double averageLength;     // Box length
    private Double averageWidth;      // Box width
    private Double averageHeight;     // Box height
    private String dimensionUnit;     // IN or CM

    public PackageProfile() {}

    public Double getAverageWeight() { return averageWeight; }
    public void setAverageWeight(Double averageWeight) { this.averageWeight = averageWeight; }

    public String getWeightUnit() { return weightUnit; }
    public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }

    public Double getAverageLength() { return averageLength; }
    public void setAverageLength(Double averageLength) { this.averageLength = averageLength; }

    public Double getAverageWidth() { return averageWidth; }
    public void setAverageWidth(Double averageWidth) { this.averageWidth = averageWidth; }

    public Double getAverageHeight() { return averageHeight; }
    public void setAverageHeight(Double averageHeight) { this.averageHeight = averageHeight; }

    public String getDimensionUnit() { return dimensionUnit; }
    public void setDimensionUnit(String dimensionUnit) { this.dimensionUnit = dimensionUnit; }
}
