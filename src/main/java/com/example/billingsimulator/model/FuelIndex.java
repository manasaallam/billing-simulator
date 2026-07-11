package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_index")
public class FuelIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fuel_program", nullable = false)
    private String fuelProgram;

    @Column(name = "week_code", nullable = false)
    private String weekCode;

    @Column(name = "fuel_index", nullable = false, precision = 6, scale = 3)
    private BigDecimal fuelIndexValue;

    // Surcharge % applied to net transportation (e.g. 14.75 = 14.75%)
    @Column(name = "fuel_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal fuelPct;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFuelProgram() { return fuelProgram; }
    public void setFuelProgram(String fuelProgram) { this.fuelProgram = fuelProgram; }
    public String getWeekCode() { return weekCode; }
    public void setWeekCode(String weekCode) { this.weekCode = weekCode; }
    public BigDecimal getFuelIndexValue() { return fuelIndexValue; }
    public void setFuelIndexValue(BigDecimal fuelIndexValue) { this.fuelIndexValue = fuelIndexValue; }
    public BigDecimal getFuelPct() { return fuelPct; }
    public void setFuelPct(BigDecimal fuelPct) { this.fuelPct = fuelPct; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
