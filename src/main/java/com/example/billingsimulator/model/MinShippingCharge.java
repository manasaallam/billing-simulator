package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "min_shipping_charge")
public class MinShippingCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, unique = true)
    private String serviceCode;

    @Column(name = "floor_zone", nullable = false)
    private Integer floorZone;

    @Column(name = "floor_weight_lb", nullable = false, precision = 6, scale = 2)
    private BigDecimal floorWeightLb;

    // Extra % off applied to the floor (e.g. THREE_DAY gets 25% additional incentive)
    @Column(name = "addl_incentive_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal addlIncentivePct = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public Integer getFloorZone() { return floorZone; }
    public void setFloorZone(Integer floorZone) { this.floorZone = floorZone; }
    public BigDecimal getFloorWeightLb() { return floorWeightLb; }
    public void setFloorWeightLb(BigDecimal floorWeightLb) { this.floorWeightLb = floorWeightLb; }
    public BigDecimal getAddlIncentivePct() { return addlIncentivePct; }
    public void setAddlIncentivePct(BigDecimal addlIncentivePct) { this.addlIncentivePct = addlIncentivePct; }
}
