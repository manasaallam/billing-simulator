package com.example.billingsimulator.service;

import com.example.billingsimulator.model.SimulationParameters;
import com.example.billingsimulator.service.ai.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Extracts structured SimulationParameters from a natural-language query.
 *
 * Delegates to AiClient (rule-based in dev, Gemini on hackathon day).
 * The system prompt tells the AI exactly what JSON schema to produce.
 */
@Service
public class ParameterExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ParameterExtractionService.class);

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a UPS billing parameter extractor. Given a customer's natural-language query
            about shipping changes, extract structured parameters as JSON.

            Schema:
            {
              "serviceShifts": [{"fromService": "string", "toService": "string", "percentage": number}],
              "volumeChange": {"changeType": "ABSOLUTE|PERCENTAGE", "changeValue": number, "frequency": "DAILY|WEEKLY|MONTHLY", "serviceLevel": "string|null"},
              "packageProfile": {"averageWeight": number, "weightUnit": "LB|KG", "averageLength": number, "averageWidth": number, "averageHeight": number, "dimensionUnit": "IN|CM"},
              "fuelChange": {"fuelPriceChangePct": number, "targetFuelSurchargePct": number, "targetDieselPrice": number},
              "deliveryTypeChange": {"residentialChangePct": number, "targetResidentialPct": number},
              "accessorialChanges": [{"surchargeCode": "string", "surchargeName": "string", "changePct": number}],
              "timeframePeriod": "WEEKLY|MONTHLY|QUARTERLY|ANNUALLY"
            }

            Rules:
            - Only include fields explicitly mentioned in the query
            - Service levels: Ground, Express, Next Day Air, 2nd Day Air, 3 Day Select, SurePost, Standard
            - Surcharge codes: DAS (Delivery Area), AH (Additional Handling), DS (Demand), SAT (Saturday), DV (Declared Value), LPS (Large Package), PAF (Premier Air Fee)
            - "Double" means +100%, "triple" means +200%
            - Negative values for decreases
            - Return ONLY valid JSON, no explanation text
            """;

    public ParameterExtractionService(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Extract structured parameters from a natural-language query.
     * Returns empty SimulationParameters if extraction fails (validation will catch it).
     */
    public SimulationParameters extract(String naturalLanguageQuery) {
        log.info("Extracting parameters from: {}", naturalLanguageQuery);

        String jsonResponse = aiClient.generate(SYSTEM_PROMPT, naturalLanguageQuery);

        try {
            SimulationParameters params = objectMapper.readValue(jsonResponse, SimulationParameters.class);
            log.info("Successfully extracted parameters");
            return params;
        } catch (Exception e) {
            log.error("Failed to deserialize extracted parameters: {}", jsonResponse, e);
            return new SimulationParameters();
        }
    }
}
