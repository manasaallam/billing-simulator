package com.example.billingsimulator.model;

import java.util.List;

/**
 * AI-generated explanation of invoice charges.
 *
 * Example response:
 *   explanation: "Your fuel surcharge is $1,077.34 because the diesel index
 *                 rose to $5.21/gallon (week 2026-W25), mapping to 14.75%..."
 *   relevantCharges: [{name: "Fuel Surcharge", amount: 1077.34, ...}]
 *   sources: ["Fuel Schedule 2026", "Contract Section 4.2"]
 */
public class InvoiceExplanation {

    private String explanation;                    // Business-friendly AI explanation
    private List<ChargeDetail> relevantCharges;   // Specific charges referenced
    private List<String> sources;                  // Knowledge sources used (for transparency)
    private String confidenceLevel;               // HIGH, MEDIUM, LOW

    public InvoiceExplanation() {}

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<ChargeDetail> getRelevantCharges() { return relevantCharges; }
    public void setRelevantCharges(List<ChargeDetail> relevantCharges) { this.relevantCharges = relevantCharges; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
}
