package com.example.billingsimulator.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Unified simulation request covering all scenario types.
 *
 * scenarioType drives which SimulationService method is invoked.
 * Only the fields relevant to the chosen scenario need to be populated.
 */
public class RateSimulationRequest {

    // ---- Common -------------------------------------------------------
    @NotBlank(message = "contractId is required")
    private String contractId;

    // UUID of the baseline_snapshot to project from.
    // If blank, the latest baseline for the account is used.
    private String baselineId;

    // Scenario discriminator — must match one of the /api/simulate/* paths
    @NotBlank(message = "scenarioType is required")
    private String scenarioType;

    // ---- VOLUME_CHANGE ------------------------------------------------
    // New average weekly shipment volume to project
    private Integer newWeeklyVolume;

    // ---- SERVICE_SHIFT ------------------------------------------------
    // Shift a portion of volume from one service to another
    private String fromService;
    private String toService;
    // 0.0–1.0 fraction of baseline volume to shift (e.g. 0.30 = 30%)
    private BigDecimal shiftFraction;

    // ---- PACKAGE_PROFILE ----------------------------------------------
    // Projected average package characteristics
    private BigDecimal newAvgWeightLb;
    private BigDecimal newAvgLengthIn;
    private BigDecimal newAvgWidthIn;
    private BigDecimal newAvgHeightIn;

    // ---- ZONE_MIX -----------------------------------------------------
    // Map of zone (as String) → fraction of total volume (values must sum to 1.0)
    // Example: {"2": 0.20, "5": 0.50, "8": 0.30}
    private Map<String, BigDecimal> zoneDistribution;

    // ---- ACCESSORIAL --------------------------------------------------
    // Accessorial codes to add to every shipment in the projection
    private List<String> addAccessorialCodes;
    // Accessorial codes to remove from every shipment in the projection
    private List<String> removeAccessorialCodes;

    // ---- FUEL_CHANGE --------------------------------------------------
    // Hypothetical fuel surcharge % to model (e.g. 18.0 = 18%)
    private BigDecimal hypotheticalFuelPct;

    // ---- COMBINED -----------------------------------------------------
    // Multiple changes at once — populate any combination of the fields above

    // ---- COMPARE ------------------------------------------------------
    // List of named sub-scenarios to compare side-by-side
    private List<NamedScenario> compareScenarios;

    // -----------------------------------------------------------------------

    public static class NamedScenario {
        private String label;
        private RateSimulationRequest request;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public RateSimulationRequest getRequest() { return request; }
        public void setRequest(RateSimulationRequest request) { this.request = request; }
    }

    // Getters & setters

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getBaselineId() { return baselineId; }
    public void setBaselineId(String baselineId) { this.baselineId = baselineId; }
    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }
    public Integer getNewWeeklyVolume() { return newWeeklyVolume; }
    public void setNewWeeklyVolume(Integer newWeeklyVolume) { this.newWeeklyVolume = newWeeklyVolume; }
    public String getFromService() { return fromService; }
    public void setFromService(String fromService) { this.fromService = fromService; }
    public String getToService() { return toService; }
    public void setToService(String toService) { this.toService = toService; }
    public BigDecimal getShiftFraction() { return shiftFraction; }
    public void setShiftFraction(BigDecimal shiftFraction) { this.shiftFraction = shiftFraction; }
    public BigDecimal getNewAvgWeightLb() { return newAvgWeightLb; }
    public void setNewAvgWeightLb(BigDecimal newAvgWeightLb) { this.newAvgWeightLb = newAvgWeightLb; }
    public BigDecimal getNewAvgLengthIn() { return newAvgLengthIn; }
    public void setNewAvgLengthIn(BigDecimal newAvgLengthIn) { this.newAvgLengthIn = newAvgLengthIn; }
    public BigDecimal getNewAvgWidthIn() { return newAvgWidthIn; }
    public void setNewAvgWidthIn(BigDecimal newAvgWidthIn) { this.newAvgWidthIn = newAvgWidthIn; }
    public BigDecimal getNewAvgHeightIn() { return newAvgHeightIn; }
    public void setNewAvgHeightIn(BigDecimal newAvgHeightIn) { this.newAvgHeightIn = newAvgHeightIn; }
    public Map<String, BigDecimal> getZoneDistribution() { return zoneDistribution; }
    public void setZoneDistribution(Map<String, BigDecimal> zoneDistribution) { this.zoneDistribution = zoneDistribution; }
    public List<String> getAddAccessorialCodes() { return addAccessorialCodes; }
    public void setAddAccessorialCodes(List<String> addAccessorialCodes) { this.addAccessorialCodes = addAccessorialCodes; }
    public List<String> getRemoveAccessorialCodes() { return removeAccessorialCodes; }
    public void setRemoveAccessorialCodes(List<String> removeAccessorialCodes) { this.removeAccessorialCodes = removeAccessorialCodes; }
    public BigDecimal getHypotheticalFuelPct() { return hypotheticalFuelPct; }
    public void setHypotheticalFuelPct(BigDecimal hypotheticalFuelPct) { this.hypotheticalFuelPct = hypotheticalFuelPct; }
    public List<NamedScenario> getCompareScenarios() { return compareScenarios; }
    public void setCompareScenarios(List<NamedScenario> compareScenarios) { this.compareScenarios = compareScenarios; }
}
