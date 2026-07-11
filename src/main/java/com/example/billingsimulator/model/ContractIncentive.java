package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "contract_incentive")
public class ContractIncentive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private String contractId;

    // ACCESSORIAL_REDUCE | FUEL_CAP | FLAT_CREDIT
    @Column(name = "incentive_type", nullable = false)
    private String incentiveType;

    // Populated for ACCESSORIAL_REDUCE rows (e.g. RESIDENTIAL, DELIVERY_AREA)
    @Column(name = "accessorial_code")
    private String accessorialCode;

    // 0.40 = 40% off; or flat USD credit amount
    @Column(name = "value", nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    // PCT | USD
    @Column(name = "unit", nullable = false)
    private String unit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getIncentiveType() { return incentiveType; }
    public void setIncentiveType(String incentiveType) { this.incentiveType = incentiveType; }
    public String getAccessorialCode() { return accessorialCode; }
    public void setAccessorialCode(String accessorialCode) { this.accessorialCode = accessorialCode; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
