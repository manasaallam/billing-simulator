package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pricing_program")
public class PricingProgram {

    @Id
    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "name", nullable = false)
    private String name;

    // FLAT | VOLUME_TIERED
    @Column(name = "type", nullable = false)
    private String type;

    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
