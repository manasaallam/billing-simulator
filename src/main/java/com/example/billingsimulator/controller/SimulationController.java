package com.example.billingsimulator.controller;

import com.example.billingsimulator.dto.SimulationRequest;
import com.example.billingsimulator.dto.SimulationResponse;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Single dispatch endpoint — AI sends scenarioType inside the request body.
     * This is the only endpoint the AI needs to know about for simulations.
     */
    @PostMapping
    public ResponseEntity<SimulationResponse> dispatch(@Valid @RequestBody SimulationRequest request) {
        if (request.getScenarioType() == null || request.getScenarioType().isBlank()) {
            throw new InvalidInputException(
                    "scenarioType is required. Valid values: VOLUME_CHANGE, SERVICE_SHIFT, " +
                    "PACKAGE_PROFILE, ZONE_MIX, ACCESSORIAL, FUEL_CHANGE, COMBINED, OPTIMIZE, COMPARE");
        }
        return ResponseEntity.ok(simulationService.dispatch(request));
    }

    // -----------------------------------------------------------------------
    // Individual sub-path endpoints (still available; scenarioType auto-set)
    // -----------------------------------------------------------------------

    @PostMapping("/volume-change")
    public ResponseEntity<SimulationResponse> volumeChange(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("VOLUME_CHANGE");
        return ResponseEntity.ok(simulationService.volumeChange(request));
    }

    @PostMapping("/service-shift")
    public ResponseEntity<SimulationResponse> serviceShift(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("SERVICE_SHIFT");
        return ResponseEntity.ok(simulationService.serviceShift(request));
    }

    @PostMapping("/package-profile")
    public ResponseEntity<SimulationResponse> packageProfile(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("PACKAGE_PROFILE");
        return ResponseEntity.ok(simulationService.packageProfile(request));
    }

    @PostMapping("/zone-mix")
    public ResponseEntity<SimulationResponse> zoneMix(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("ZONE_MIX");
        return ResponseEntity.ok(simulationService.zoneMix(request));
    }

    @PostMapping("/accessorial")
    public ResponseEntity<SimulationResponse> accessorial(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("ACCESSORIAL");
        return ResponseEntity.ok(simulationService.accessorialChange(request));
    }

    @PostMapping("/fuel-change")
    public ResponseEntity<SimulationResponse> fuelChange(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("FUEL_CHANGE");
        return ResponseEntity.ok(simulationService.fuelChange(request));
    }

    @PostMapping("/combined")
    public ResponseEntity<SimulationResponse> combined(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("COMBINED");
        return ResponseEntity.ok(simulationService.combined(request));
    }

    @PostMapping("/optimize")
    public ResponseEntity<SimulationResponse> optimize(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("OPTIMIZE");
        return ResponseEntity.ok(simulationService.optimize(request));
    }

    @PostMapping("/compare")
    public ResponseEntity<SimulationResponse> compare(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("COMPARE");
        return ResponseEntity.ok(simulationService.compare(request));
    }
}

 *
 * POST /api/simulate/volume-change    — what if I ship more/fewer packages per week?
 * POST /api/simulate/service-shift    — what if I move volume from GROUND to THREE_DAY?
 * POST /api/simulate/package-profile  — what if my average package gets heavier?
 * POST /api/simulate/zone-mix         — what if more deliveries go to zone 8?
 * POST /api/simulate/accessorial      — what if I eliminate residential surcharges?
 * POST /api/simulate/fuel-change      — what if fuel surcharge goes to 18%?
 * POST /api/simulate/combined         — multiple changes at once
 * POST /api/simulate/optimize         — how much more do I need to ship to hit the next tier?
 * POST /api/simulate/compare          — side-by-side comparison of named scenarios
 */
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/volume-change")
    public ResponseEntity<SimulationResponse> volumeChange(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("VOLUME_CHANGE");
        return ResponseEntity.ok(simulationService.volumeChange(request));
    }

    @PostMapping("/service-shift")
    public ResponseEntity<SimulationResponse> serviceShift(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("SERVICE_SHIFT");
        return ResponseEntity.ok(simulationService.serviceShift(request));
    }

    @PostMapping("/package-profile")
    public ResponseEntity<SimulationResponse> packageProfile(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("PACKAGE_PROFILE");
        return ResponseEntity.ok(simulationService.packageProfile(request));
    }

    @PostMapping("/zone-mix")
    public ResponseEntity<SimulationResponse> zoneMix(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("ZONE_MIX");
        return ResponseEntity.ok(simulationService.zoneMix(request));
    }

    @PostMapping("/accessorial")
    public ResponseEntity<SimulationResponse> accessorial(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("ACCESSORIAL");
        return ResponseEntity.ok(simulationService.accessorialChange(request));
    }

    @PostMapping("/fuel-change")
    public ResponseEntity<SimulationResponse> fuelChange(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("FUEL_CHANGE");
        return ResponseEntity.ok(simulationService.fuelChange(request));
    }

    @PostMapping("/combined")
    public ResponseEntity<SimulationResponse> combined(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("COMBINED");
        return ResponseEntity.ok(simulationService.combined(request));
    }

    @PostMapping("/optimize")
    public ResponseEntity<SimulationResponse> optimize(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("OPTIMIZE");
        return ResponseEntity.ok(simulationService.optimize(request));
    }

    @PostMapping("/compare")
    public ResponseEntity<SimulationResponse> compare(@Valid @RequestBody SimulationRequest request) {
        request.setScenarioType("COMPARE");
        return ResponseEntity.ok(simulationService.compare(request));
    }
}
