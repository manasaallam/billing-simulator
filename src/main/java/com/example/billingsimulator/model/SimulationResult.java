package com.example.billingsimulator.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Output from the rate engine simulation (other team implements the logic).
 * Contains the invoice breakdown: current vs projected.
 *
 * Mirrors the document's output format:
 *   Current Invoice:    $502,000
 *   Projected Invoice:  $449,000
 *   Savings:            $53,000
 *
 * With breakdown by charge category (Transportation, Fuel, Residential, etc.)
 */
public class SimulationResult {

    private String simulationId;
    private BigDecimal currentInvoiceTotal;
    private BigDecimal projectedInvoiceTotal;
    private BigDecimal estimatedSavings;
    private Double savingsPercentage;
    private Map<String, BigDecimal> currentBreakdown;      // e.g., {"Transportation": 420000, "Fuel": 58000, ...}
    private Map<String, BigDecimal> projectedBreakdown;    // Same keys, projected values
    private String confidenceLevel;   // HIGH, MEDIUM, LOW
    private String explanation;        // Gemini-generated business explanation

    public SimulationResult() {}

    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }

    public BigDecimal getCurrentInvoiceTotal() { return currentInvoiceTotal; }
    public void setCurrentInvoiceTotal(BigDecimal currentInvoiceTotal) { this.currentInvoiceTotal = currentInvoiceTotal; }

    public BigDecimal getProjectedInvoiceTotal() { return projectedInvoiceTotal; }
    public void setProjectedInvoiceTotal(BigDecimal projectedInvoiceTotal) { this.projectedInvoiceTotal = projectedInvoiceTotal; }

    public BigDecimal getEstimatedSavings() { return estimatedSavings; }
    public void setEstimatedSavings(BigDecimal estimatedSavings) { this.estimatedSavings = estimatedSavings; }

    public Double getSavingsPercentage() { return savingsPercentage; }
    public void setSavingsPercentage(Double savingsPercentage) { this.savingsPercentage = savingsPercentage; }

    public Map<String, BigDecimal> getCurrentBreakdown() { return currentBreakdown; }
    public void setCurrentBreakdown(Map<String, BigDecimal> currentBreakdown) { this.currentBreakdown = currentBreakdown; }

    public Map<String, BigDecimal> getProjectedBreakdown() { return projectedBreakdown; }
    public void setProjectedBreakdown(Map<String, BigDecimal> projectedBreakdown) { this.projectedBreakdown = projectedBreakdown; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
