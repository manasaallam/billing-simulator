package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.RateSimulationRequest;
import com.example.billingsimulator.model.RateSimulationResponse;
import com.example.billingsimulator.exception.ContractNotFoundException;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.model.AppUser;
import com.example.billingsimulator.repository.AppUserRepository;
import com.example.billingsimulator.repository.ContractRepository;
import com.example.billingsimulator.service.ParameterExtractionService;
import com.example.billingsimulator.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Simulation scenario endpoints.
 *
 * PRIMARY ENTRY POINT (AI uses this after parameter extraction):
 *   POST /api/rate/simulate   — send scenarioType in the body; backend dispatches automatically
 *
 * Valid scenarioType values:
 *   VOLUME_CHANGE | SERVICE_SHIFT | PACKAGE_PROFILE | ZONE_MIX |
 *   ACCESSORIAL   | FUEL_CHANGE   | COMBINED        | OPTIMIZE | COMPARE
 *
 * Individual sub-paths (/api/rate/simulate/volume-change etc.) are also available
 * and do not require scenarioType in the body.
 *
 * NOTE: /api/simulate is owned by the AI/chat team (NL text → extraction → clarification).
 *       This controller handles only structured-param calculation requests.
 */
@RestController
@RequestMapping("/api/rate/simulate")
public class RateSimulationController {

    private final SimulationService simulationService;
    private final ParameterExtractionService extractionService;
    private final AppUserRepository userRepo;
    private final ContractRepository contractRepo;

    public RateSimulationController(SimulationService simulationService,
                                     ParameterExtractionService extractionService,
                                     AppUserRepository userRepo,
                                     ContractRepository contractRepo) {
        this.simulationService = simulationService;
        this.extractionService = extractionService;
        this.userRepo = userRepo;
        this.contractRepo = contractRepo;
    }

    /** Sets the AI-generated plain-English explanation on the response before returning. */
    private RateSimulationResponse withExplanation(RateSimulationResponse resp) {
        resp.setExplanation(extractionService.explainResult(resp));
        return resp;
    }

    /** Resolves the caller's active contractId from the JWT userId. */
    private String resolveContractId(Authentication auth) {
        AppUser user = userRepo.findById(UUID.fromString(auth.getName())).orElseThrow();
        return contractRepo.findByCompanyId(user.getCompanyId()).stream()
                .findFirst()
                .orElseThrow(() -> new ContractNotFoundException("No contract found for company"))
                .getContractId();
    }

    /**
     * Single dispatch endpoint — AI sends scenarioType inside the request body.
     * This is the only endpoint the AI needs to know about for simulations.
     */
    @PostMapping
    public ResponseEntity<RateSimulationResponse> dispatch(@Valid @RequestBody RateSimulationRequest request,
                                                            Authentication auth) {
        if (request.getScenarioType() == null || request.getScenarioType().isBlank()) {
            throw new InvalidInputException(
                    "scenarioType is required. Valid values: VOLUME_CHANGE, SERVICE_SHIFT, " +
                    "PACKAGE_PROFILE, ZONE_MIX, ACCESSORIAL, FUEL_CHANGE, COMBINED, OPTIMIZE, COMPARE");
        }
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.dispatch(request)));
    }

    // -----------------------------------------------------------------------
    // Individual sub-path endpoints (still available; scenarioType auto-set)
    // -----------------------------------------------------------------------

    @PostMapping("/volume-change")
    public ResponseEntity<RateSimulationResponse> volumeChange(@Valid @RequestBody RateSimulationRequest request,
                                                                Authentication auth) {
        request.setScenarioType("VOLUME_CHANGE");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.volumeChange(request)));
    }

    @PostMapping("/service-shift")
    public ResponseEntity<RateSimulationResponse> serviceShift(@Valid @RequestBody RateSimulationRequest request,
                                                                Authentication auth) {
        request.setScenarioType("SERVICE_SHIFT");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.serviceShift(request)));
    }

    @PostMapping("/package-profile")
    public ResponseEntity<RateSimulationResponse> packageProfile(@Valid @RequestBody RateSimulationRequest request,
                                                                  Authentication auth) {
        request.setScenarioType("PACKAGE_PROFILE");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.packageProfile(request)));
    }

    @PostMapping("/zone-mix")
    public ResponseEntity<RateSimulationResponse> zoneMix(@Valid @RequestBody RateSimulationRequest request,
                                                           Authentication auth) {
        request.setScenarioType("ZONE_MIX");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.zoneMix(request)));
    }

    @PostMapping("/accessorial")
    public ResponseEntity<RateSimulationResponse> accessorial(@Valid @RequestBody RateSimulationRequest request,
                                                               Authentication auth) {
        request.setScenarioType("ACCESSORIAL");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.accessorialChange(request)));
    }

    @PostMapping("/fuel-change")
    public ResponseEntity<RateSimulationResponse> fuelChange(@Valid @RequestBody RateSimulationRequest request,
                                                              Authentication auth) {
        request.setScenarioType("FUEL_CHANGE");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.fuelChange(request)));
    }

    @PostMapping("/combined")
    public ResponseEntity<RateSimulationResponse> combined(@Valid @RequestBody RateSimulationRequest request,
                                                            Authentication auth) {
        request.setScenarioType("COMBINED");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.combined(request)));
    }

    @PostMapping("/optimize")
    public ResponseEntity<RateSimulationResponse> optimize(@Valid @RequestBody RateSimulationRequest request,
                                                            Authentication auth) {
        request.setScenarioType("OPTIMIZE");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.optimize(request)));
    }

    @PostMapping("/compare")
    public ResponseEntity<RateSimulationResponse> compare(@Valid @RequestBody RateSimulationRequest request,
                                                           Authentication auth) {
        request.setScenarioType("COMPARE");
        request.setContractId(resolveContractId(auth));
        return ResponseEntity.ok(withExplanation(simulationService.compare(request)));
    }
}
