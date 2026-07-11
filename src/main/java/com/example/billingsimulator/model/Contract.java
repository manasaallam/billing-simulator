package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "tier")
    private String tier;

    @Column(name = "fuel_program", nullable = false)
    private String fuelProgram;

    @Column(name = "payment_terms")
    private String paymentTerms;

    // % per month on overdue balance (e.g. 0.0100 = 1%)
    @Column(name = "late_payment_fee_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal latePaymentFeePct;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_system")
    private String sourceSystem;

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getFuelProgram() { return fuelProgram; }
    public void setFuelProgram(String fuelProgram) { this.fuelProgram = fuelProgram; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public BigDecimal getLatePaymentFeePct() { return latePaymentFeePct; }
    public void setLatePaymentFeePct(BigDecimal latePaymentFeePct) { this.latePaymentFeePct = latePaymentFeePct; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
}
