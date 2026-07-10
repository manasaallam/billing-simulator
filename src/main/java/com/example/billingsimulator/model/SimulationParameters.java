package com.example.billingsimulator.model;

import java.util.List;

/**
 * Container for all extracted simulation scenario parameters.
 * Produced by ParameterExtractionService, validated by ParameterValidationService.
 *
 * A single query can populate multiple fields:
 *   "Shift 30% of Air to Ground and reduce residential by 20%"
 *   → serviceShifts: [{from: "Air", to: "Ground", pct: 30}]
 *   → deliveryTypeChange: {residentialChangePct: -20.0}
 *
 * At least ONE field must be non-null for a valid simulation.
 * All fields are nullable — null means "no change in this dimension."
 */
public class SimulationParameters {

    private List<ServiceShift> serviceShifts;           // Service level changes (can be multiple)
    private VolumeChange volumeChange;                  // Shipping volume change
    private PackageProfile packageProfile;              // Package weight/dimension change
    private FuelChange fuelChange;                      // Fuel price/surcharge change
    private DeliveryTypeChange deliveryTypeChange;      // Residential vs commercial mix
    private List<AccessorialChange> accessorialChanges; // Surcharge changes (can be multiple)
    private String timeframePeriod;                     // WEEKLY, MONTHLY, QUARTERLY, ANNUALLY

    public SimulationParameters() {}

    public List<ServiceShift> getServiceShifts() { return serviceShifts; }
    public void setServiceShifts(List<ServiceShift> serviceShifts) { this.serviceShifts = serviceShifts; }

    public VolumeChange getVolumeChange() { return volumeChange; }
    public void setVolumeChange(VolumeChange volumeChange) { this.volumeChange = volumeChange; }

    public PackageProfile getPackageProfile() { return packageProfile; }
    public void setPackageProfile(PackageProfile packageProfile) { this.packageProfile = packageProfile; }

    public FuelChange getFuelChange() { return fuelChange; }
    public void setFuelChange(FuelChange fuelChange) { this.fuelChange = fuelChange; }

    public DeliveryTypeChange getDeliveryTypeChange() { return deliveryTypeChange; }
    public void setDeliveryTypeChange(DeliveryTypeChange deliveryTypeChange) { this.deliveryTypeChange = deliveryTypeChange; }

    public List<AccessorialChange> getAccessorialChanges() { return accessorialChanges; }
    public void setAccessorialChanges(List<AccessorialChange> accessorialChanges) { this.accessorialChanges = accessorialChanges; }

    public String getTimeframePeriod() { return timeframePeriod; }
    public void setTimeframePeriod(String timeframePeriod) { this.timeframePeriod = timeframePeriod; }

    /**
     * Returns true if at least one scenario dimension is present.
     */
    public boolean hasAnyScenario() {
        return (serviceShifts != null && !serviceShifts.isEmpty())
                || volumeChange != null
                || packageProfile != null
                || fuelChange != null
                || deliveryTypeChange != null
                || (accessorialChanges != null && !accessorialChanges.isEmpty());
    }
}
