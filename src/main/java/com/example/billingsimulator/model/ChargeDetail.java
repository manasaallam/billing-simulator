package com.example.billingsimulator.model;

import java.math.BigDecimal;

/**
 * A single charge line from an invoice.
 * Mirrors real UPS invoice structure from the sample documents.
 *
 * Example:
 *   { name: "Fuel Surcharge", code: "FUEL", amount: 5.51,
 *     description: "14.75% of transportation charges based on diesel index" }
 */
public class ChargeDetail {

    private String name;            // Human-readable (e.g., "Fuel Surcharge")
    private String code;            // System code (e.g., "FUEL", "DAS", "RES")
    private BigDecimal amount;      // Dollar amount
    private String description;     // How this charge is calculated
    private String trackingNumber;  // Optional: which shipment this applies to
    private String serviceType;     // Optional: Ground, Express, etc.

    public ChargeDetail() {}

    public ChargeDetail(String name, String code, BigDecimal amount, String description) {
        this.name = name;
        this.code = code;
        this.amount = amount;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
}
