package com.example.billingsimulator.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gemini AI client using Google AI Studio / Vertex AI.
 * Activated only when gemini.api-key is set.
 * Takes priority over LocalRuleBasedAiClient when active.
 *
 * Get API key: https://aistudio.google.com/apikey
 */
@Component
@Primary
@ConditionalOnProperty(name = "gemini.api-key")
public class GeminiAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String explanationPrompt;

    public GeminiAiClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("classpath:prompts/explanation-prompt.txt") Resource explanationPromptResource) {
        this.apiKey = apiKey;
        this.model = model;
        try {
            this.explanationPrompt = explanationPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load explanation-prompt.txt", e);
        }
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        log.info("Gemini AI Client configured: model={}", model);
    }

    @Override
    public String generate(String systemPrompt, String userMessage) {
        log.info("Calling Gemini model: {}", model);

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractContent(response);
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            return "{}";
        }
    }

    @Override
    public String explain(String resultsJson) {
        log.info("Calling Gemini for NL explanation");

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", explanationPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", resultsJson)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7
                )
        );

        try {
            String response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractContent(response);
        } catch (Exception e) {
            log.error("Gemini explanation call failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String clarify(String originalQuery, String validationErrors) {
        log.info("Calling Gemini for clarification questions");

        String systemPrompt = """
                You are a UPS billing assistant. The customer asked a shipping question but some details are missing or ambiguous.
                Given their original query and the validation issues, generate helpful clarification questions.

                Return JSON array:
                [
                  {
                    "field": "fieldName",
                    "question": "Natural, conversational question to ask the customer",
                    "suggestedOptions": ["option1", "option2", ...]
                  }
                ]

                Rules:
                - Keep questions short, friendly, and specific to what's missing
                - Provide 3-6 suggested options where applicable
                - Use plain business language, not technical codes
                - Maximum 3 questions
                - Return ONLY valid JSON array
                """;

        String userMessage = "Original query: " + originalQuery + "\nValidation issues: " + validationErrors;

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractContent(response);
        } catch (Exception e) {
            log.error("Gemini clarification call failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
            log.info("Gemini response: {}", text);
            return text;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response, e);
            return "{}";
        }
    }
}
