package com.example.billingsimulator.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Projection result returned by SimulationService.
 * All money values are USD computed by the deterministic Java rate engine.
 * The AI layer uses this to phrase the plain-English answer — it never modifies the numbers.
 */
public class RateSimulationResponse {

    private String scenarioType;
    private String contractId;
    private String baselineId;

    // ---- Baseline (what the account currently pays) -------------------
    private BigDecimal baselineWeeklyCost;
    private BigDecimal baselineAnnualCost;
    private BigDecimal baselineAvgWeeklyVolume;
    private String baselineDiscountTierBand;

    // ---- Projection (what the account would pay under the new scenario)
    private BigDecimal projectedWeeklyCost;
    private BigDecimal projectedAnnualCost;
    private String projectedDiscountTierBand;

    // ---- Delta --------------------------------------------------------
    private BigDecimal weeklyDelta;          // projected − baseline (negative = savings)
    private BigDecimal annualDelta;
    private BigDecimal deltaPct;             // % change

    // ---- Breakdown (for itemised display) ----------------------------
    private Map<String, BigDecimal> baselineCostByCategory;   // GROUND, AIR, ...
    private Map<String, BigDecimal> projectedCostByCategory;

    // ---- Scenario-specific detail ------------------------------------
    // Volume change: tier upgrade info
    private String tierUpgradeNote;

    // Service shift: per-service impact
    private List<LineItem> serviceShiftBreakdown;

    // Optimize: list of next-tier thresholds
    private List<OptimizationHint> optimizationHints;

    // Compare: side-by-side labels + deltas
    private List<CompareEntry> compareEntries;

    // ---- Audit --------------------------------------------------------
    private String rateCardVersion;
    private String billWeek;
    private String confidence;   // HIGH | MEDIUM | LOW
    private String caveat;

    // -----------------------------------------------------------------------

    public static class OptimizationHint {
        private String category;
        private int currentVolume;
        private int nextTierVolume;
        private BigDecimal currentDiscountPct;
        private BigDecimal nextDiscountPct;
        private BigDecimal estimatedAnnualSavings;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getCurrentVolume() { return currentVolume; }
        public void setCurrentVolume(int currentVolume) { this.currentVolume = currentVolume; }
        public int getNextTierVolume() { return nextTierVolume; }
        public void setNextTierVolume(int nextTierVolume) { this.nextTierVolume = nextTierVolume; }
        public BigDecimal getCurrentDiscountPct() { return currentDiscountPct; }
        public void setCurrentDiscountPct(BigDecimal currentDiscountPct) { this.currentDiscountPct = currentDiscountPct; }
        public BigDecimal getNextDiscountPct() { return nextDiscountPct; }
        public void setNextDiscountPct(BigDecimal nextDiscountPct) { this.nextDiscountPct = nextDiscountPct; }
        public BigDecimal getEstimatedAnnualSavings() { return estimatedAnnualSavings; }
        public void setEstimatedAnnualSavings(BigDecimal estimatedAnnualSavings) { this.estimatedAnnualSavings = estimatedAnnualSavings; }
    }

    public static class CompareEntry {
        private String label;
        private BigDecimal projectedAnnualCost;
        private BigDecimal annualDelta;
        private BigDecimal deltaPct;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public BigDecimal getProjectedAnnualCost() { return projectedAnnualCost; }
        public void setProjectedAnnualCost(BigDecimal projectedAnnualCost) { this.projectedAnnualCost = projectedAnnualCost; }
        public BigDecimal getAnnualDelta() { return annualDelta; }
        public void setAnnualDelta(BigDecimal annualDelta) { this.annualDelta = annualDelta; }
        public BigDecimal getDeltaPct() { return deltaPct; }
        public void setDeltaPct(BigDecimal deltaPct) { this.deltaPct = deltaPct; }
    }

    // Getters & setters

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getBaselineId() { return baselineId; }
    public void setBaselineId(String baselineId) { this.baselineId = baselineId; }
    public BigDecimal getBaselineWeeklyCost() { return baselineWeeklyCost; }
    public void setBaselineWeeklyCost(BigDecimal baselineWeeklyCost) { this.baselineWeeklyCost = baselineWeeklyCost; }
    public BigDecimal getBaselineAnnualCost() { return baselineAnnualCost; }
    public void setBaselineAnnualCost(BigDecimal baselineAnnualCost) { this.baselineAnnualCost = baselineAnnualCost; }
    public BigDecimal getBaselineAvgWeeklyVolume() { return baselineAvgWeeklyVolume; }
    public void setBaselineAvgWeeklyVolume(BigDecimal baselineAvgWeeklyVolume) { this.baselineAvgWeeklyVolume = baselineAvgWeeklyVolume; }
    public String getBaselineDiscountTierBand() { return baselineDiscountTierBand; }
    public void setBaselineDiscountTierBand(String baselineDiscountTierBand) { this.baselineDiscountTierBand = baselineDiscountTierBand; }
    public BigDecimal getProjectedWeeklyCost() { return projectedWeeklyCost; }
    public void setProjectedWeeklyCost(BigDecimal projectedWeeklyCost) { this.projectedWeeklyCost = projectedWeeklyCost; }
    public BigDecimal getProjectedAnnualCost() { return projectedAnnualCost; }
    public void setProjectedAnnualCost(BigDecimal projectedAnnualCost) { this.projectedAnnualCost = projectedAnnualCost; }
    public String getProjectedDiscountTierBand() { return projectedDiscountTierBand; }
    public void setProjectedDiscountTierBand(String projectedDiscountTierBand) { this.projectedDiscountTierBand = projectedDiscountTierBand; }
    public BigDecimal getWeeklyDelta() { return weeklyDelta; }
    public void setWeeklyDelta(BigDecimal weeklyDelta) { this.weeklyDelta = weeklyDelta; }
    public BigDecimal getAnnualDelta() { return annualDelta; }
    public void setAnnualDelta(BigDecimal annualDelta) { this.annualDelta = annualDelta; }
    public BigDecimal getDeltaPct() { return deltaPct; }
    public void setDeltaPct(BigDecimal deltaPct) { this.deltaPct = deltaPct; }
    public Map<String, BigDecimal> getBaselineCostByCategory() { return baselineCostByCategory; }
    public void setBaselineCostByCategory(Map<String, BigDecimal> baselineCostByCategory) { this.baselineCostByCategory = baselineCostByCategory; }
    public Map<String, BigDecimal> getProjectedCostByCategory() { return projectedCostByCategory; }
    public void setProjectedCostByCategory(Map<String, BigDecimal> projectedCostByCategory) { this.projectedCostByCategory = projectedCostByCategory; }
    public String getTierUpgradeNote() { return tierUpgradeNote; }
    public void setTierUpgradeNote(String tierUpgradeNote) { this.tierUpgradeNote = tierUpgradeNote; }
    public List<LineItem> getServiceShiftBreakdown() { return serviceShiftBreakdown; }
    public void setServiceShiftBreakdown(List<LineItem> serviceShiftBreakdown) { this.serviceShiftBreakdown = serviceShiftBreakdown; }
    public List<OptimizationHint> getOptimizationHints() { return optimizationHints; }
    public void setOptimizationHints(List<OptimizationHint> optimizationHints) { this.optimizationHints = optimizationHints; }
    public List<CompareEntry> getCompareEntries() { return compareEntries; }
    public void setCompareEntries(List<CompareEntry> compareEntries) { this.compareEntries = compareEntries; }
    public String getRateCardVersion() { return rateCardVersion; }
    public void setRateCardVersion(String rateCardVersion) { this.rateCardVersion = rateCardVersion; }
    public String getBillWeek() { return billWeek; }
    public void setBillWeek(String billWeek) { this.billWeek = billWeek; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getCaveat() { return caveat; }
    public void setCaveat(String caveat) { this.caveat = caveat; }
}
