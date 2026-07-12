package com.example.billingsimulator.controller;

import com.example.billingsimulator.model.InvoiceExplanation;
import com.example.billingsimulator.model.InvoiceQuery;
import com.example.billingsimulator.service.InvoiceExplainerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Smart Invoice Explainer.
 *
 * Endpoint:
 *   POST /api/explain → Ask a question about invoice charges
 */
@RestController
@RequestMapping("/api/explain")
public class InvoiceExplainerController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExplainerController.class);

    private final InvoiceExplainerService explainerService;

    public InvoiceExplainerController(InvoiceExplainerService explainerService) {
        this.explainerService = explainerService;
    }

    @PostMapping
    public ResponseEntity<InvoiceExplanation> explain(@Valid @RequestBody InvoiceQuery query) {
        log.info("Invoice explanation request from customer: {}", query.getCustomerId());
        InvoiceExplanation explanation = explainerService.explain(query);
        return ResponseEntity.ok(explanation);
    }
}
