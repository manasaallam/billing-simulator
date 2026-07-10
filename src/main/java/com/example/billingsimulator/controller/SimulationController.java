package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.*;
import com.example.billingsimulator.service.ParameterExtractionService;
import com.example.billingsimulator.service.ParameterValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for billing simulations.
 *
 * Endpoints:
 *   POST /api/simulate         → New simulation from natural-language query
 *   POST /api/simulate/clarify → Re-run with clarification answers
 *
 * Flow:
 *   1. Extract parameters from NL (AI)
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

        // Step 1: Extract parameters from natural language
        SimulationParameters params = extractionService.extract(request.getNaturalLanguageQuery());

        // Step 2: Validate extracted parameters
        List<ClarificationQuestion> questions = validationService.validate(params);

        if (!questions.isEmpty()) {
            // Parameters are incomplete/ambiguous — ask for clarification
            log.info("Clarification needed: {} question(s)", questions.size());
            return ResponseEntity.ok(
                    SimulationResponse.needsClarification(questions, conversationId)
            );
        }

        // Step 3: Parameters valid — run simulation (TODO: rate engine integration)
        log.info("Parameters valid — running simulation");
        SimulationResult result = buildMockResult(params);

        return ResponseEntity.ok(
                SimulationResponse.success(result, conversationId)
        );
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

        // Parameters come pre-structured from the UI (user answered clarification questions)
        SimulationParameters params = request.getExtractedParameters();

        // Validate again
        List<ClarificationQuestion> questions = validationService.validate(params);

        if (!questions.isEmpty()) {
            return ResponseEntity.ok(
                    SimulationResponse.needsClarification(questions, conversationId)
            );
        }

        // Valid — run simulation
        SimulationResult result = buildMockResult(params);

        return ResponseEntity.ok(
                SimulationResponse.success(result, conversationId)
        );
    }

    /**
     * Mock result until rate engine is integrated (other team's work).
     * Returns a placeholder result so the full flow can be tested end-to-end.
     */
    private SimulationResult buildMockResult(SimulationParameters params) {
        SimulationResult result = new SimulationResult();
        result.setSimulationId(UUID.randomUUID().toString());
        result.setCurrentInvoiceTotal(new java.math.BigDecimal("502000"));
        result.setProjectedInvoiceTotal(new java.math.BigDecimal("449000"));
        result.setEstimatedSavings(new java.math.BigDecimal("53000"));
        result.setSavingsPercentage(10.56);
        result.setConfidenceLevel("MEDIUM");
        result.setExplanation("Based on your scenario, the projected monthly invoice could decrease by approximately $53,000. "
                + "This is a projection based on historical patterns and is not a final quote.");
        return result;
    }
}
