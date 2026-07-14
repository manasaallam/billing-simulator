package com.example.billingsimulator.service.ai;

/**
 * Abstraction for AI/LLM calls.
 *
 * Implementations:
 *   - LocalRuleBasedAiClient (dev profile) — regex patterns, no API key needed
 *   - GeminiAiClient (gemini profile)      — real NL understanding via Gemini API
 *
 * Switch between them using spring.profiles.active:
 *   dev    → LocalRuleBasedAiClient
 *   gemini → GeminiAiClient
 */
public interface AiClient {

    /**
     * Send a system prompt + user message to the AI and get a JSON response.
     *
     * @param systemPrompt instructions for the AI (schema, rules, examples)
     * @param userMessage  the customer's natural-language query
     * @return JSON string representing extracted SimulationParameters
     */
    String generate(String systemPrompt, String userMessage);

    /**
     * Take a rate engine result JSON and return a plain-English business explanation.
     *
     * @param resultsJson  serialized RateSimulationResponse from the rate engine
     * @return plain-text explanation suitable for display to the customer
     */
    String explain(String resultsJson);

    /**
     * Generate contextual clarification questions when extracted parameters are
     * incomplete or ambiguous.
     *
     * @param originalQuery     the user's original natural-language query
     * @param validationErrors  JSON describing what's missing/invalid
     * @return JSON array of clarification questions
     */
    String clarify(String originalQuery, String validationErrors);
}
