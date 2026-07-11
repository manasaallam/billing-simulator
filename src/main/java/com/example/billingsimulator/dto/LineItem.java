package com.example.billingsimulator.dto;

import java.math.BigDecimal;

/** A single charge line on a rate quote or invoice breakdown. */
public class LineItem {
    private String code;
    private String description;
    private BigDecimal publishedAmount;
    private BigDecimal amount;

    public LineItem() {}

    public LineItem(String code, String description, BigDecimal publishedAmount, BigDecimal amount) {
        this.code = code;
        this.description = description;
        this.publishedAmount = publishedAmount;
        this.amount = amount;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPublishedAmount() { return publishedAmount; }
    public void setPublishedAmount(BigDecimal publishedAmount) { this.publishedAmount = publishedAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
