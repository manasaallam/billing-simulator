package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accessorial_type")
public class AccessorialType {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "default_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultFee;

    @Column(name = "trigger_rule")
    private String triggerRule;

    @Column(name = "apply_basis", nullable = false)
    private String applyBasis;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BigDecimal getDefaultFee() { return defaultFee; }
    public void setDefaultFee(BigDecimal defaultFee) { this.defaultFee = defaultFee; }
    public String getTriggerRule() { return triggerRule; }
    public void setTriggerRule(String triggerRule) { this.triggerRule = triggerRule; }
    public String getApplyBasis() { return applyBasis; }
    public void setApplyBasis(String applyBasis) { this.applyBasis = applyBasis; }
}
