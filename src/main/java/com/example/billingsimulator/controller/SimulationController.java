package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.*;
import com.example.billingsimulator.service.ParameterExtractionService;
import com.example.billingsimulator.service.ParameterValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for billing simulations.
 *
 * Endpoints:
 *   POST /api/simulate         → New simulation from natural-language query
 *   POST /api/simulate/clarify → Re-run with clarification answers
 *
 * Flow:
 *   1. Extract parameters from NL (AI) — or use pre-extracted params
 *   2. Validate parameters (Java rules)
 *   3. If invalid → return clarification questions
 *   4. If valid → run simulation (future: rate engine)
 */
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final ParameterExtractionService extractionService;
    private final ParameterValidationService validationService;

    public SimulationController(ParameterExtractionService extractionService,
                                ParameterValidationService validationService) {
        this.extractionService = extractionService;
        this.validationService = validationService;
    }

    /**
     * New simulation — takes a natural-language query, extracts and validates parameters.
     */
    @PostMapping
    public ResponseEntity<SimulationResponse> simulate(@Valid @RequestBody SimulationRequest request) {
        log.info("Simulation request from customer: {}", request.getCustomerId());

        String conversationId = UUID.randomUUID().toString();

        try {
            // Step 1: Extract parameters (or use pre-extracted if provided)
            SimulationParameters params;
            if (request.getExtractedParameters() != null && request.getExtractedParameters().hasAnyScenario()) {
                params = request.getExtractedParameters();
                log.info("Using pre-extracted parameters");
            } else {
                params = extractionService.extract(request.getNaturalLanguageQuery());
            }

            // Steps 2-4: Validate and run
            return ResponseEntity.ok(validateAndSimulate(params, conversationId));

        } catch (Exception e) {
            log.error("Simulation failed", e);
            return ResponseEntity.ok(SimulationResponse.error("Something went wrong. Please try again."));
        }
    }

    /**
     * Clarification flow — customer answered follow-up questions,
     * parameters are already structured (no AI extraction needed).
     */
    @PostMapping("/clarify")
    public ResponseEntity<SimulationResponse> clarify(@Valid @RequestBody SimulationRequest request) {
        log.info("Clarification response from customer: {}", request.getCustomerId());

        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        try {
            SimulationParameters params = request.getExtractedParameters();

            // Fail fast if no parameters provided in clarification
            if (params == null) {
                return ResponseEntity.ok(SimulationResponse.error("No parameters provided in clarification."));
            }

            return ResponseEntity.ok(validateAndSimulate(params, conversationId));

        } catch (Exception e) {
            log.error("Clarification failed", e);
            return ResponseEntity.ok(SimulationResponse.error("Something went wrong. Please try again."));
        }
    }

    /**
     * Common logic: validate parameters → return clarification or simulation result.
     */
    private SimulationResponse validateAndSimulate(SimulationParameters params, String conversationId) {
        // Step 2: Validate extracted parameters
        List<ClarificationQuestion> questions = validationService.validate(params);

        if (!questions.isEmpty()) {
            log.info("Clarification needed: {} question(s)", questions.size());
            return SimulationResponse.needsClarification(questions, conversationId);
        }

        // Step 3: Parameters valid — run simulation
        // TODO: Replace mock with real rate engine:
        //   CustomerBaseline baseline = baselineService.getBaseline(customerId);
        //   SimulationResult result = rateEngineService.runSimulation(baseline, params);
        log.info("Parameters valid — running simulation");
        SimulationResult result = buildMockResult(params);

        return SimulationResponse.success(result, conversationId);
    }

    /**
     * Mock result — varies based on scenario type for a more realistic demo.
     * Replace with real rate engine on hackathon day.
     */
    private SimulationResult buildMockResult(SimulationParameters params) {
        SimulationResult result = new SimulationResult();
        result.setSimulationId(UUID.randomUUID().toString());

        BigDecimal currentTotal = new BigDecimal("502000");
        BigDecimal projectedTotal;
        String explanation;

        // Vary mock result based on what scenario was requested
        if (params.getServiceShifts() != null && !params.getServiceShifts().isEmpty()) {
            projectedTotal = new BigDecimal("449000");
            explanation = buildServiceShiftExplanation(params);
            result.setCurrentBreakdown(Map.of(
                    "Transportation", new BigDecimal("420000"),
                    "Fuel Surcharge", new BigDecimal("58000"),
                    "Residential", new BigDecimal("24000")
            ));
            result.setProjectedBreakdown(Map.of(
                    "Transportation", new BigDecimal("376000"),
                    "Fuel Surcharge", new BigDecimal("49000"),
                    "Residential", new BigDecimal("24000")
            ));
        } else if (params.getVolumeChange() != null) {
            projectedTotal = new BigDecimal("602000");
            explanation = "Based on the volume change, your projected monthly invoice would increase to approximately $602,000. "
                    + "Higher volume may qualify you for additional tier discounts — contact your account manager.";
            result.setCurrentBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Residential", new BigDecimal("24000")));
            result.setProjectedBreakdown(Map.of("Transportation", new BigDecimal("504000"), "Fuel Surcharge", new BigDecimal("70000"), "Residential", new BigDecimal("28000")));
        } else if (params.getFuelChange() != null) {
            projectedTotal = new BigDecimal("518000");
            explanation = "With the projected fuel price change, your fuel surcharge component would increase, "
                    + "raising the monthly invoice by approximately $16,000.";
            result.setCurrentBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Residential", new BigDecimal("24000")));
            result.setProjectedBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("74000"), "Residential", new BigDecimal("24000")));
        } else if (params.getDeliveryTypeChange() != null) {
            projectedTotal = new BigDecimal("486000");
            explanation = "Reducing residential deliveries would lower your Residential Surcharge exposure, "
                    + "saving approximately $16,000 per month.";
            result.setCurrentBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Residential", new BigDecimal("24000")));
            result.setProjectedBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Residential", new BigDecimal("8000")));
        } else if (params.getAccessorialChanges() != null && !params.getAccessorialChanges().isEmpty()) {
            projectedTotal = new BigDecimal("478000");
            explanation = "Eliminating the specified surcharge exposure could save approximately $24,000 per month.";
            result.setCurrentBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Accessorials", new BigDecimal("24000")));
            result.setProjectedBreakdown(Map.of("Transportation", new BigDecimal("420000"), "Fuel Surcharge", new BigDecimal("58000"), "Accessorials", new BigDecimal("0")));
        } else {
            projectedTotal = new BigDecimal("449000");
            explanation = "Based on your scenario, the projected monthly invoice could decrease by approximately $53,000.";
        }

        result.setCurrentInvoiceTotal(currentTotal);
        result.setProjectedInvoiceTotal(projectedTotal);
        result.setEstimatedSavings(currentTotal.subtract(projectedTotal));
        result.setSavingsPercentage(currentTotal.subtract(projectedTotal)
                .multiply(new BigDecimal("100"))
                .divide(currentTotal, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue());
        result.setConfidenceLevel("MEDIUM");
        result.setExplanation(explanation + " This is a projection based on historical patterns and is not a final quote.");

        return result;
    }

    private String buildServiceShiftExplanation(SimulationParameters params) {
        ServiceShift shift = params.getServiceShifts().get(0);
        return String.format("Shifting %s%% of %s shipments to %s could reduce your projected monthly cost by approximately $53,000. "
                        + "The savings come from lower base transportation rates and reduced fuel surcharge exposure.",
                shift.getPercentage() != null ? shift.getPercentage().intValue() : "some",
                shift.getFromService() != null ? shift.getFromService() : "current service",
                shift.getToService() != null ? shift.getToService() : "target service");
    }
}
