package com.example.billingsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "discount_category")
public class DiscountCategory {

    @Id
    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
