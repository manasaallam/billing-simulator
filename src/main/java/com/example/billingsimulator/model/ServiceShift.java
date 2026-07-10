package com.example.billingsimulator.model;

/**
 * Represents a shift of shipment volume from one service level to another.
 *
 * Examples:
 *   "Move 20% of Express to Ground"   → { fromService: "Express", toService: "Ground", percentage: 20.0 }
 *   "Switch all to Next Day Air"       → { fromService: null, toService: "Next Day Air", percentage: 100.0 }
 */
public class ServiceShift {

    private String fromService;   // Source service level (e.g., "Express")
    private String toService;     // Target service level (e.g., "Ground")
    private Double percentage;    // Percentage of shipments to shift (1-100)

    public ServiceShift() {}

    public ServiceShift(String fromService, String toService, Double percentage) {
        this.fromService = fromService;
        this.toService = toService;
        this.percentage = percentage;
    }

    public String getFromService() { return fromService; }
    public void setFromService(String fromService) { this.fromService = fromService; }

    public String getToService() { return toService; }
    public void setToService(String toService) { this.toService = toService; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
}
