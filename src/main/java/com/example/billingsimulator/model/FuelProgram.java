package com.example.billingsimulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fuel_program")
public class FuelProgram {

    @Id
    @Column(name = "fuel_program")
    private String fuelProgram;

    @Column(name = "index_basis", nullable = false)
    private String indexBasis;

    @Column(name = "formula_note")
    private String formulaNote;

    public String getFuelProgram() { return fuelProgram; }
    public void setFuelProgram(String fuelProgram) { this.fuelProgram = fuelProgram; }
    public String getIndexBasis() { return indexBasis; }
    public void setIndexBasis(String indexBasis) { this.indexBasis = indexBasis; }
    public String getFormulaNote() { return formulaNote; }
    public void setFormulaNote(String formulaNote) { this.formulaNote = formulaNote; }
}
