package com.example.billingsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dim_factor")
public class DimFactor {

    @Id
    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "divisor", nullable = false)
    private Integer divisor;

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public Integer getDivisor() { return divisor; }
    public void setDivisor(Integer divisor) { this.divisor = divisor; }
}
