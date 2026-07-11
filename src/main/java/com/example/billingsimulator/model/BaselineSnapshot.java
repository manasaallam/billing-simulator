package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "baseline_snapshot")
public class BaselineSnapshot {

    @Id
    @Column(name = "baseline_id")
    private UUID baselineId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "total_shipments")
    private Integer totalShipments;

    @Column(name = "avg_weekly_volume", precision = 8, scale = 2)
    private BigDecimal avgWeeklyVolume;

    @Column(name = "total_cost", precision = 14, scale = 2)
    private BigDecimal totalCost;

    // JSONB stored as text — parsed by SimulationService with ObjectMapper.
    // Structure: { weekly_volume_avg, spend_by_service, spend_by_zone,
    //              avg_weight_lbs, residential_pct, accessorial_pct }
    @Column(name = "metrics_json", columnDefinition = "jsonb")
    private String metricsJson;

    public UUID getBaselineId() { return baselineId; }
    public void setBaselineId(UUID baselineId) { this.baselineId = baselineId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public Integer getTotalShipments() { return totalShipments; }
    public void setTotalShipments(Integer totalShipments) { this.totalShipments = totalShipments; }
    public BigDecimal getAvgWeeklyVolume() { return avgWeeklyVolume; }
    public void setAvgWeeklyVolume(BigDecimal avgWeeklyVolume) { this.avgWeeklyVolume = avgWeeklyVolume; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
}
