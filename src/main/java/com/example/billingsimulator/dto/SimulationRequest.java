package com.example.billingsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {

    private ScenarioType scenarioType;

    private String sourceService;

    private String targetService;

    private String service;

    private Double percentage;

    private Double currentWeight;

    private Double newWeight;

    private Double fuelIncrease;

    private String effectivePeriod;

    private Integer currentVolume;

    private Integer newVolume;

    private String volumeUnit;
}