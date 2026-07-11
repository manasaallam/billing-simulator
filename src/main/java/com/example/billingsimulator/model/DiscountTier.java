package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "discount_tier")
public class DiscountTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    // AIR | GROUND | INTL_EXPRESS_EXPORT | INTL_EXPRESS_IMPORT | INTL_STANDARD
    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "vol_min", nullable = false)
    private Integer volMin;

    // NULL = open-ended top band (e.g. 71+)
    @Column(name = "vol_max")
    private Integer volMax;

    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountPct;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Integer getVolMin() { return volMin; }
    public void setVolMin(Integer volMin) { this.volMin = volMin; }
    public Integer getVolMax() { return volMax; }
    public void setVolMax(Integer volMax) { this.volMax = volMax; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
}
