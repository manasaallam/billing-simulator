package com.example.billingsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "zone_matrix")
public class ZoneMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_prefix", nullable = false)
    private String originPrefix;

    @Column(name = "dest_prefix", nullable = false)
    private String destPrefix;

    @Column(name = "zone", nullable = false)
    private Integer zone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginPrefix() { return originPrefix; }
    public void setOriginPrefix(String originPrefix) { this.originPrefix = originPrefix; }
    public String getDestPrefix() { return destPrefix; }
    public void setDestPrefix(String destPrefix) { this.destPrefix = destPrefix; }
    public Integer getZone() { return zone; }
    public void setZone(Integer zone) { this.zone = zone; }
}
