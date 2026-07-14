package com.example.billingsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_level")
public class ServiceLevel {

    @Id
    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "transit_days")
    private Integer transitDays;

    @Column(name = "is_air", nullable = false)
    private Boolean isAir = false;

    // Stored as FK string — join resolved by RateEngineService when needed
    @Column(name = "discount_category")
    private String discountCategory;

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Integer getTransitDays() { return transitDays; }
    public void setTransitDays(Integer transitDays) { this.transitDays = transitDays; }
    public Boolean getIsAir() { return isAir; }
    public void setIsAir(Boolean isAir) { this.isAir = isAir; }
    public String getDiscountCategory() { return discountCategory; }
    public void setDiscountCategory(String discountCategory) { this.discountCategory = discountCategory; }
}
