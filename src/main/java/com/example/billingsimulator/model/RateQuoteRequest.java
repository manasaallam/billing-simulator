package com.example.billingsimulator.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Input parameters for a single-package rate quote request.
 */
public class RateQuoteRequest {

    @NotBlank(message = "contractId is required")
    private String contractId;

    @NotBlank(message = "serviceCode is required")
    private String serviceCode;

    @NotBlank(message = "originZip is required")
    private String originZip;

    @NotBlank(message = "destZip is required")
    private String destZip;

    @NotNull(message = "actualWeightLb is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 lb")
    private BigDecimal actualWeightLb;

    // Optional — dimensional weight calculation (L × W × H / divisor)
    private BigDecimal lengthIn;
    private BigDecimal widthIn;
    private BigDecimal heightIn;

    private boolean residential = false;

    private BigDecimal declaredValue;

    // ISO week: '2026-W25'. Defaults to current week if blank.
    private String billWeek;

    private int packageCount = 1;

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getOriginZip() { return originZip; }
    public void setOriginZip(String originZip) { this.originZip = originZip; }
    public String getDestZip() { return destZip; }
    public void setDestZip(String destZip) { this.destZip = destZip; }
    public BigDecimal getActualWeightLb() { return actualWeightLb; }
    public void setActualWeightLb(BigDecimal actualWeightLb) { this.actualWeightLb = actualWeightLb; }
    public BigDecimal getLengthIn() { return lengthIn; }
    public void setLengthIn(BigDecimal lengthIn) { this.lengthIn = lengthIn; }
    public BigDecimal getWidthIn() { return widthIn; }
    public void setWidthIn(BigDecimal widthIn) { this.widthIn = widthIn; }
    public BigDecimal getHeightIn() { return heightIn; }
    public void setHeightIn(BigDecimal heightIn) { this.heightIn = heightIn; }
    public boolean isResidential() { return residential; }
    public void setResidential(boolean residential) { this.residential = residential; }
    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }
    public String getBillWeek() { return billWeek; }
    public void setBillWeek(String billWeek) { this.billWeek = billWeek; }
    public int getPackageCount() { return packageCount; }
    public void setPackageCount(int packageCount) { this.packageCount = packageCount; }
}
