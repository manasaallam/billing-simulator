package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.*;
import com.example.billingsimulator.dto.RateSimulationRequest;
import com.example.billingsimulator.dto.RateSimulationResponse;
import com.example.billingsimulator.service.ParameterExtractionService;
import com.example.billingsimulator.service.ParameterValidationService;
import com.example.billingsimulator.service.SimulationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final SimulationService simulationService;

    public SimulationController(ParameterExtractionService extractionService,
                                ParameterValidationService validationService,
                                SimulationService simulationService) {
        this.extractionService = extractionService;
        this.validationService = validationService;
        this.simulationService = simulationService;
    }

    /**
     * New simulation — takes a natural-language query, extracts and validates parameters.
     */
    @PostMapping
    public ResponseEntity<SimulationResponse> simulate(@Valid @RequestBody SimulationRequest request) {
        log.info("Simulation request from contract: {}", request.getContractId());

        String conversationId = UUID.randomUUID().toString();

        try {
            SimulationParameters params;
            if (request.getExtractedParameters() != null && request.getExtractedParameters().hasAnyScenario()) {
                params = request.getExtractedParameters();
                log.info("Using pre-extracted parameters");
            } else {
                params = extractionService.extract(request.getNaturalLanguageQuery());
            }

            return ResponseEntity.ok(validateAndSimulate(params, conversationId, request.getContractId()));

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
        log.info("Clarification response from contract: {}", request.getContractId());

        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        try {
            SimulationParameters params = request.getExtractedParameters();
            if (params == null) {
                return ResponseEntity.ok(SimulationResponse.error("No parameters provided in clarification."));
            }
            return ResponseEntity.ok(validateAndSimulate(params, conversationId, request.getContractId()));
        } catch (Exception e) {
            log.error("Clarification failed", e);
            return ResponseEntity.ok(SimulationResponse.error("Something went wrong. Please try again."));
        }
    }

    private SimulationResponse validateAndSimulate(SimulationParameters params, String conversationId, String contractId) {
        List<ClarificationQuestion> questions = validationService.validate(params);
        if (!questions.isEmpty()) {
            log.info("Clarification needed: {} question(s)", questions.size());
            return SimulationResponse.needsClarification(questions, conversationId);
        }

        log.info("Parameters valid — calling rate engine for contract {}", contractId);
        RateSimulationRequest rateReq = mapToRateRequest(params, contractId);
        RateSimulationResponse rateResp = simulationService.dispatch(rateReq);
        SimulationResult result = mapToResult(rateResp);
        return SimulationResponse.success(result, conversationId);
    }

    // --- Mapper: AI params → Rate engine request ---

    private RateSimulationRequest mapToRateRequest(SimulationParameters params, String contractId) {
        RateSimulationRequest req = new RateSimulationRequest();
        req.setContractId(contractId);

        if (params.getServiceShifts() != null && !params.getServiceShifts().isEmpty()) {
            ServiceShift shift = params.getServiceShifts().get(0);
            req.setScenarioType("SERVICE_SHIFT");
            req.setFromService(shift.getFromService());
            req.setToService(shift.getToService());
            if (shift.getPercentage() != null) {
                req.setShiftFraction(BigDecimal.valueOf(shift.getPercentage() / 100.0));
            }
        } else if (params.getVolumeChange() != null) {
            VolumeChange vc = params.getVolumeChange();
            req.setScenarioType("VOLUME_CHANGE");
            int baseline = 20; // CTR-001 demo baseline weekly volume
            int newVol;
            if ("PERCENTAGE".equals(vc.getChangeType()) && vc.getChangeValue() != null) {
                newVol = (int) Math.round(baseline * (1 + vc.getChangeValue() / 100.0));
            } else {
                double val = vc.getChangeValue() != null ? vc.getChangeValue() : baseline;
                if ("DAILY".equals(vc.getFrequency())) val *= 5;
                else if ("MONTHLY".equals(vc.getFrequency())) val /= 4.0;
                newVol = (int) Math.round(val);
            }
            req.setNewWeeklyVolume(Math.max(1, newVol));
        } else if (params.getPackageProfile() != null) {
            PackageProfile pp = params.getPackageProfile();
            req.setScenarioType("PACKAGE_PROFILE");
            if (pp.getAverageWeight() != null) {
                double lb = "KG".equalsIgnoreCase(pp.getWeightUnit()) ? pp.getAverageWeight() * 2.205 : pp.getAverageWeight();
                req.setNewAvgWeightLb(BigDecimal.valueOf(lb));
            }
            if (pp.getAverageLength() != null) {
                double f = "CM".equalsIgnoreCase(pp.getDimensionUnit()) ? 0.3937 : 1.0;
                req.setNewAvgLengthIn(BigDecimal.valueOf(pp.getAverageLength() * f));
                if (pp.getAverageWidth() != null)  req.setNewAvgWidthIn(BigDecimal.valueOf(pp.getAverageWidth() * f));
                if (pp.getAverageHeight() != null) req.setNewAvgHeightIn(BigDecimal.valueOf(pp.getAverageHeight() * f));
            }
        } else if (params.getFuelChange() != null) {
            FuelChange fc = params.getFuelChange();
            req.setScenarioType("FUEL_CHANGE");
            if (fc.getTargetFuelSurchargePct() != null) {
                req.setHypotheticalFuelPct(BigDecimal.valueOf(fc.getTargetFuelSurchargePct()));
            }
        } else if (params.getDeliveryTypeChange() != null || (params.getAccessorialChanges() != null && !params.getAccessorialChanges().isEmpty())) {
            req.setScenarioType("ACCESSORIAL");
            List<String> add = new ArrayList<>();
            List<String> remove = new ArrayList<>();
            if (params.getAccessorialChanges() != null) {
                for (AccessorialChange ac : params.getAccessorialChanges()) {
                    if (ac.getSurchargeCode() == null) continue;
                    if (ac.getChangePct() != null && ac.getChangePct() <= -50) remove.add(ac.getSurchargeCode());
                    else if (ac.getChangePct() != null && ac.getChangePct() > 0)  add.add(ac.getSurchargeCode());
                }
            }
            if (params.getDeliveryTypeChange() != null && params.getDeliveryTypeChange().getResidentialChangePct() != null) {
                if (params.getDeliveryTypeChange().getResidentialChangePct() < 0) remove.add("RESIDENTIAL");
                else add.add("RESIDENTIAL");
            }
            if (!remove.isEmpty()) req.setRemoveAccessorialCodes(remove);
            if (!add.isEmpty())    req.setAddAccessorialCodes(add);
        } else {
            // Fallback: no recognised scenario — default to volume change with no change
            req.setScenarioType("VOLUME_CHANGE");
            req.setNewWeeklyVolume(20);
        }
        return req;
    }

    // --- Mapper: Rate engine response → SimulationResult ---

    private SimulationResult mapToResult(RateSimulationResponse resp) {
        SimulationResult result = new SimulationResult();
        result.setSimulationId(UUID.randomUUID().toString());
        result.setCurrentInvoiceTotal(resp.getBaselineAnnualCost());
        result.setProjectedInvoiceTotal(resp.getProjectedAnnualCost());
        if (resp.getAnnualDelta() != null) {
            // annualDelta = projected − baseline (negative means savings)
            result.setEstimatedSavings(resp.getAnnualDelta().negate());
        }
        if (resp.getDeltaPct() != null) {
            result.setSavingsPercentage(-resp.getDeltaPct().doubleValue());
        }
        result.setConfidenceLevel(resp.getConfidence() != null ? resp.getConfidence().toUpperCase() : "MEDIUM");
        result.setExplanation(resp.getCaveat());
        return result;
    }
}
