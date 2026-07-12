package com.example.billingsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIResponse {

    private String status; // READY or NEEDS_MORE_INFORMATION

    private String message;

    private SimulationRequest simulationRequest;

    private List<MissingField> missingFields;

    // getters/setters
}
