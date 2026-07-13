package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.RateQuoteRequest;
import com.example.billingsimulator.model.RateQuoteResponse;
import com.example.billingsimulator.exception.ContractNotFoundException;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.model.Contract;
import com.example.billingsimulator.repository.BaselineSnapshotRepository;
import com.example.billingsimulator.repository.ContractRepository;
import com.example.billingsimulator.service.RateEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rate calculation endpoints.
 *
 * POST /api/rate/quote       — single-package rate quote
 * POST /api/rate/quote/batch — batch of up to 50 packages
 * GET  /api/rate/card        — list available rate card versions
 */
@RestController
@RequestMapping("/api/rate")
public class RateController {

    private final RateEngineService rateEngine;
    private final ContractRepository contractRepo;
    private final BaselineSnapshotRepository baselineRepo;

    public RateController(RateEngineService rateEngine,
                          ContractRepository contractRepo,
                          BaselineSnapshotRepository baselineRepo) {
        this.rateEngine = rateEngine;
        this.contractRepo = contractRepo;
        this.baselineRepo = baselineRepo;
    }

    /**
     * Single-package rate quote.
     *
     * Required fields: contractId, serviceCode, originZip, destZip, actualWeightLb.
     * The avgWeeklyVolume for the discount tier lookup is resolved server-side
     * from the account's latest baseline snapshot.
     */
    @PostMapping("/quote")
    public ResponseEntity<RateQuoteResponse> quote(@Valid @RequestBody RateQuoteRequest request) {
        int avgWeeklyVolume = resolveAvgWeeklyVolume(request.getContractId());
        RateQuoteResponse response = rateEngine.quote(request, avgWeeklyVolume);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch rate quote — up to 50 packages per call.
     * Each package can have different service, weight, dims, and accessorials.
     */
    @PostMapping("/quote/batch")
    public ResponseEntity<List<RateQuoteResponse>> quoteBatch(
            @Valid @RequestBody List<RateQuoteRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidInputException("At least one request is required");
        }
        if (requests.size() > 50) {
            throw new InvalidInputException("Batch size cannot exceed 50 packages");
        }
        // Resolve volume once for the first contract (all requests in a batch share the same account)
        int avgWeeklyVolume = resolveAvgWeeklyVolume(requests.get(0).getContractId());
        List<RateQuoteResponse> responses = requests.stream()
                .map(req -> rateEngine.quote(req, avgWeeklyVolume))
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * List available rate card versions — useful for audit and debugging.
     */
    @GetMapping("/card")
    public ResponseEntity<List<String>> rateCardVersions() {
        List<String> versions = rateEngine.getRateCardVersions();
        return ResponseEntity.ok(versions);
    }

    // -----------------------------------------------------------------------

    private int resolveAvgWeeklyVolume(String contractId) {
        Contract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException("Contract not found: " + contractId));
        return baselineRepo.findLatest(contract.getCompanyId())
                .map(b -> b.getAvgWeeklyVolume().intValue())
                .orElse(0);
    }
}
