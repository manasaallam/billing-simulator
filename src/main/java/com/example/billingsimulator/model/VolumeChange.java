package com.example.billingsimulator.model;

/**
 * Represents a change in shipping volume.
 *
 * Examples:
 *   "Increase volume by 20%"                → { changeType: PERCENTAGE, changeValue: 20.0, frequency: MONTHLY }
 *   "Add 500 packages per week"             → { changeType: ABSOLUTE, changeValue: 500.0, frequency: WEEKLY }
 *   "Holiday volume doubles"                → { changeType: PERCENTAGE, changeValue: 100.0, frequency: MONTHLY }
 *   "Reduce Ground shipments by 10%"        → { changeType: PERCENTAGE, changeValue: -10.0, serviceLevel: "Ground" }
 */
public class VolumeChange {

    private String changeType;     // ABSOLUTE or PERCENTAGE
    private Double changeValue;    // Positive = increase, negative = decrease
    private String frequency;      // DAILY, WEEKLY, MONTHLY (period over which change applies)
    private String serviceLevel;   // Optional: if change applies to specific service only (e.g., "Ground")

    public VolumeChange() {}

    public VolumeChange(String changeType, Double changeValue, String frequency, String serviceLevel) {
        this.changeType = changeType;
        this.changeValue = changeValue;
        this.frequency = frequency;
        this.serviceLevel = serviceLevel;
    }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public Double getChangeValue() { return changeValue; }
    public void setChangeValue(Double changeValue) { this.changeValue = changeValue; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
}
