package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rate_card")
public class RateCard {

    @Id
    @Column(name = "rate_card_id")
    private UUID rateCardId;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    @Column(name = "zone", nullable = false)
    private Integer zone;

    @Column(name = "weight_from_lb", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightFromLb;

    @Column(name = "weight_to_lb", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightToLb;

    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public UUID getRateCardId() { return rateCardId; }
    public void setRateCardId(UUID rateCardId) { this.rateCardId = rateCardId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public Integer getZone() { return zone; }
    public void setZone(Integer zone) { this.zone = zone; }
    public BigDecimal getWeightFromLb() { return weightFromLb; }
    public void setWeightFromLb(BigDecimal weightFromLb) { this.weightFromLb = weightFromLb; }
    public BigDecimal getWeightToLb() { return weightToLb; }
    public void setWeightToLb(BigDecimal weightToLb) { this.weightToLb = weightToLb; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
