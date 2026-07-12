package com.example.billingsimulator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming request from the React UI.
 *
 * Two usage modes:
 * 1. Initial query:        contractId + naturalLanguageQuery (AI extracts params)
 * 2. Clarification reply:  contractId + conversationId + extractedParameters (user provided missing info)
 *
 * Example (initial):
 *   POST /api/simulate
 *   { "contractId": "CTR-001", "naturalLanguageQuery": "What if I shift 30% of Air to Ground?" }
 *
 * Example (clarification):
 *   POST /api/simulate/clarify
 *   { "contractId": "CTR-001", "conversationId": "abc-123", "extractedParameters": { ... } }
 */
public class SimulationRequest {

    @NotBlank(message = "Contract ID is required")
    private String contractId;

    @Size(min = 10, max = 2000, message = "Query must be between 10 and 2000 characters")
    private String naturalLanguageQuery;

    private SimulationParameters extractedParameters;  // Populated on clarification flow

    private String conversationId;  // Links clarification answers back to original query

    public SimulationRequest() {}

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getNaturalLanguageQuery() { return naturalLanguageQuery; }
    public void setNaturalLanguageQuery(String naturalLanguageQuery) { this.naturalLanguageQuery = naturalLanguageQuery; }

    public SimulationParameters getExtractedParameters() { return extractedParameters; }
    public void setExtractedParameters(SimulationParameters extractedParameters) { this.extractedParameters = extractedParameters; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
