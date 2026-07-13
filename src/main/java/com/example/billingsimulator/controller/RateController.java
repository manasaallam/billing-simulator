package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.RateQuoteRequest;
import com.example.billingsimulator.model.RateQuoteResponse;
import com.example.billingsimulator.exception.ContractNotFoundException;
import com.example.billingsimulator.exception.InvalidInputException;
import com.example.billingsimulator.model.AppUser;
import com.example.billingsimulator.repository.AppUserRepository;
import com.example.billingsimulator.repository.BaselineSnapshotRepository;
import com.example.billingsimulator.repository.ContractRepository;
import com.example.billingsimulator.service.RateEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    private final AppUserRepository userRepo;

    public RateController(RateEngineService rateEngine,
                          ContractRepository contractRepo,
                          BaselineSnapshotRepository baselineRepo,
                          AppUserRepository userRepo) {
        this.rateEngine = rateEngine;
        this.contractRepo = contractRepo;
        this.baselineRepo = baselineRepo;
        this.userRepo = userRepo;
    }

    /**
     * Single-package rate quote.
     *
     * Required fields: contractId, serviceCode, originZip, destZip, actualWeightLb.
     * The avgWeeklyVolume for the discount tier lookup is resolved server-side
     * from the account's latest baseline snapshot.
     */
    @PostMapping("/quote")
    public ResponseEntity<RateQuoteResponse> quote(@Valid @RequestBody RateQuoteRequest request,
                                                    Authentication auth) {
        AppUser user = userRepo.findById(UUID.fromString(auth.getName())).orElseThrow();
        request.setContractId(resolveContractId(user.getCompanyId()));
        int avgWeeklyVolume = resolveAvgWeeklyVolume(user.getCompanyId());
        return ResponseEntity.ok(rateEngine.quote(request, avgWeeklyVolume));
    }

    /**
     * Batch rate quote — up to 50 packages per call.
     * Each package can have different service, weight, dims, and accessorials.
     */
    @PostMapping("/quote/batch")
    public ResponseEntity<List<RateQuoteResponse>> quoteBatch(
            @Valid @RequestBody List<RateQuoteRequest> requests,
            Authentication auth) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidInputException("At least one request is required");
        }
        if (requests.size() > 50) {
            throw new InvalidInputException("Batch size cannot exceed 50 packages");
        }
        AppUser user = userRepo.findById(UUID.fromString(auth.getName())).orElseThrow();
        String contractId = resolveContractId(user.getCompanyId());
        int avgWeeklyVolume = resolveAvgWeeklyVolume(user.getCompanyId());
        List<RateQuoteResponse> responses = requests.stream()
                .map(req -> { req.setContractId(contractId); return rateEngine.quote(req, avgWeeklyVolume); })
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

    private String resolveContractId(UUID companyId) {
        return contractRepo.findByCompanyId(companyId).stream()
                .findFirst()
                .orElseThrow(() -> new ContractNotFoundException("No contract found for company"))
                .getContractId();
    }

    private int resolveAvgWeeklyVolume(UUID companyId) {
        return baselineRepo.findLatest(companyId)
                .map(b -> b.getAvgWeeklyVolume().intValue())
                .orElse(0);
    }
}
