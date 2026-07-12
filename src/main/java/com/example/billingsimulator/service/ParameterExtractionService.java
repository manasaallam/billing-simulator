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
            - Service levels (use EXACT codes): GROUND, GROUND_RES, EXPRESS, EXPRESS_SAVER, TWO_DAY, THREE_DAY, INTL_STANDARD, INTL_EXP_EXPORT, INTL_EXP_IMPORT
              * GROUND = Ground (4-day)
              * GROUND_RES = Ground Residential (4-day)
              * EXPRESS = Next Day Express (1-day air)
              * EXPRESS_SAVER = Express Saver (1-day air)
              * TWO_DAY = 2 Day Air
              * THREE_DAY = 3 Day Select
              * INTL_EXP_EXPORT = International Express Export
              * INTL_EXP_IMPORT = International Express Import
              * INTL_STANDARD = International Standard
            - Surcharge codes (use EXACT codes): DELIVERY_AREA, ADDL_HANDLING, DEMAND, SATURDAY, DECLARED_VALUE, PREMIUM_AIR, RESIDENTIAL, DELIVERY_AREA_EXT
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
