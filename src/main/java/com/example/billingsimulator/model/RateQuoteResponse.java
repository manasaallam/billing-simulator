package com.example.billingsimulator.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full charge breakdown for a single-package rate quote.
 * All money is computed by RateEngineService — AI never touches these values.
 */
public class RateQuoteResponse {

    private String contractId;
    private String serviceCode;
    private String serviceDisplayName;
    private int zone;
    private BigDecimal actualWeightLb;
    private BigDecimal billedWeightLb;   // max(actual, dim)
    private String weightBasis;           // "ACTUAL" | "DIM"

    // Transportation
    private BigDecimal publishedCharge;
    private BigDecimal discountPct;
    private BigDecimal discountAmount;
    private BigDecimal netTransport;
    private boolean minFloorApplied;

    // Fuel
    private BigDecimal fuelPct;
    private BigDecimal fuelCharge;

    // Accessorials
    private BigDecimal accessorialCharge;

    // Grand total
    private BigDecimal totalCharge;

    // Itemised breakdown for display / RAG explanation
    private List<LineItem> lineItems;

    // Audit fields
    private String rateCardVersion;
    private String billWeek;
    private String discountTierBand;     // e.g. "11–30 shipments/week"
    private String confidence;           // HIGH | MEDIUM | LOW
    private String caveat;

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getServiceDisplayName() { return serviceDisplayName; }
    public void setServiceDisplayName(String serviceDisplayName) { this.serviceDisplayName = serviceDisplayName; }
    public int getZone() { return zone; }
    public void setZone(int zone) { this.zone = zone; }
    public BigDecimal getActualWeightLb() { return actualWeightLb; }
    public void setActualWeightLb(BigDecimal actualWeightLb) { this.actualWeightLb = actualWeightLb; }
    public BigDecimal getBilledWeightLb() { return billedWeightLb; }
    public void setBilledWeightLb(BigDecimal billedWeightLb) { this.billedWeightLb = billedWeightLb; }
    public String getWeightBasis() { return weightBasis; }
    public void setWeightBasis(String weightBasis) { this.weightBasis = weightBasis; }
    public BigDecimal getPublishedCharge() { return publishedCharge; }
    public void setPublishedCharge(BigDecimal publishedCharge) { this.publishedCharge = publishedCharge; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getNetTransport() { return netTransport; }
    public void setNetTransport(BigDecimal netTransport) { this.netTransport = netTransport; }
    public boolean isMinFloorApplied() { return minFloorApplied; }
    public void setMinFloorApplied(boolean minFloorApplied) { this.minFloorApplied = minFloorApplied; }
    public BigDecimal getFuelPct() { return fuelPct; }
    public void setFuelPct(BigDecimal fuelPct) { this.fuelPct = fuelPct; }
    public BigDecimal getFuelCharge() { return fuelCharge; }
    public void setFuelCharge(BigDecimal fuelCharge) { this.fuelCharge = fuelCharge; }
    public BigDecimal getAccessorialCharge() { return accessorialCharge; }
    public void setAccessorialCharge(BigDecimal accessorialCharge) { this.accessorialCharge = accessorialCharge; }
    public BigDecimal getTotalCharge() { return totalCharge; }
    public void setTotalCharge(BigDecimal totalCharge) { this.totalCharge = totalCharge; }
    public List<LineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }
    public String getRateCardVersion() { return rateCardVersion; }
    public void setRateCardVersion(String rateCardVersion) { this.rateCardVersion = rateCardVersion; }
    public String getBillWeek() { return billWeek; }
    public void setBillWeek(String billWeek) { this.billWeek = billWeek; }
    public String getDiscountTierBand() { return discountTierBand; }
    public void setDiscountTierBand(String discountTierBand) { this.discountTierBand = discountTierBand; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getCaveat() { return caveat; }
    public void setCaveat(String caveat) { this.caveat = caveat; }
}
