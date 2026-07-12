package com.example.billingsimulator.util;

import com.example.billingsimulator.dto.AIResponse;
import com.example.billingsimulator.dto.MissingField;
import com.example.billingsimulator.dto.ScenarioType;
import com.example.billingsimulator.dto.SimulationRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationValidator {

    public AIResponse validate(String question, AIResponse response) {

        normalizeRelativeVolumeChanges(question, response);

        if (!"READY".equals(response.getStatus())) {
            return response;
        }

        if (response.getSimulationRequest() == null) {
            return response;
        }

        return switch (response.getSimulationRequest().getScenarioType()) {
            case SERVICE_CHANGE -> validateServiceChange(response);
            case VOLUME_CHANGE -> validateVolumeChange(response);
            case WEIGHT_CHANGE -> validateWeightChange(response);
            case FUEL_CHANGE -> validateFuelChange(response);
            case RESIDENTIAL_CHANGE -> validateResidentialChange(response);
        };
    }

    private AIResponse validateServiceChange(AIResponse response) {

        SimulationRequest request = response.getSimulationRequest();

        if (request.getSourceService() == null) {
            return buildMissingFieldResponse(
                    "sourceService",
                    "Which UPS service should be moved?"
            );
        }

        if (request.getTargetService() == null) {
            return buildMissingFieldResponse(
                    "targetService",
                    "Which UPS service should the shipments be moved to?"
            );
        }

        return response;
    }

    private AIResponse validateVolumeChange(AIResponse response) {

        SimulationRequest request = response.getSimulationRequest();

        boolean hasPercentage = request.getPercentage() != null;

        boolean hasAbsoluteVolume =
                request.getCurrentVolume() != null &&
                        request.getNewVolume() != null;

        if (!hasPercentage && !hasAbsoluteVolume) {

            return buildMissingFieldResponse(
                    "percentage",
                    "By what percentage should the shipment volume change?"
            );
        }

        return response;
    }

    private AIResponse validateWeightChange(AIResponse response) {

        SimulationRequest request = response.getSimulationRequest();

        if (request.getCurrentWeight() == null) {
            return buildMissingFieldResponse(
                    "currentWeight",
                    "What is the current package weight?"
            );
        }

        if (request.getNewWeight() == null) {
            return buildMissingFieldResponse(
                    "newWeight",
                    "What should the new package weight be?"
            );
        }

        return response;
    }

    private AIResponse validateFuelChange(AIResponse response) {

        SimulationRequest request = response.getSimulationRequest();

        if (request.getFuelIncrease() == null) {

            return buildMissingFieldResponse(
                    "fuelIncrease",
                    "By what percentage should the fuel surcharge change?"
            );
        }

        return response;
    }

    private AIResponse validateResidentialChange(AIResponse response) {
        return response;
    }

    private AIResponse buildMissingFieldResponse(String field,
                                                 String question) {

        AIResponse response = new AIResponse();

        response.setStatus("NEEDS_MORE_INFORMATION");
        response.setMessage("More information is required to run the billing simulation.");
        response.setSimulationRequest(null);

        response.setMissingFields(
                List.of(new MissingField(field, question))
        );

        return response;
    }

    private void normalizeRelativeVolumeChanges(String question,
                                                AIResponse response) {

        if (question == null
                || response == null
                || response.getSimulationRequest() == null) {
            return;
        }

        SimulationRequest request = response.getSimulationRequest();

        if (request.getScenarioType() != ScenarioType.VOLUME_CHANGE) {
            return;
        }

        if (request.getPercentage() != null) {
            return;
        }

        String q = question.toLowerCase();

        if (q.contains("double") || q.contains("doubles")) {
            request.setPercentage(100.0);
        } else if (q.contains("triple") || q.contains("triples")) {
            request.setPercentage(200.0);
        } else if (q.contains("half")
                || q.contains("halve")
                || q.contains("halved")
                || q.contains("cut in half")) {
            request.setPercentage(-50.0);
        }
    }
}
