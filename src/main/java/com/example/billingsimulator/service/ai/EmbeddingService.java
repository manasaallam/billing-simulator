package com.example.billingsimulator.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calls Gemini text-embedding-004 to convert text into a 768-dimension vector.
 * Activated only when gemini.api-key is set.
 *
 * Endpoint: POST /v1beta/models/text-embedding-004:embedContent?key={key}
 * Response: { "embedding": { "values": [0.012, -0.045, ...] } }
 */
@Service
@ConditionalOnProperty(name = "gemini.api-key")
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String EMBEDDING_MODEL = "gemini-embedding-001";
    private static final int EXPECTED_DIMENSIONS = 768;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String apiKey;

    public EmbeddingService(@Value("${gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        log.info("EmbeddingService initialized with model: {}", EMBEDDING_MODEL);
    }

    /**
     * Embed a single text string into a 768-dimension vector.
     *
     * @param text the text to embed
     * @return float array of 768 dimensions
     */
    public float[] embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                ),
                "outputDimensionality", EXPECTED_DIMENSIONS
        );

        try {
            String response = webClient.post()
                    .uri("/models/{model}:embedContent?key={key}", EMBEDDING_MODEL, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEmbedding(response);
        } catch (Exception e) {
            log.error("Embedding call failed for text ({}... chars): {}", 
                      Math.min(text.length(), 50), e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    /**
     * Convert a float[] embedding to pgvector string format: '[0.1, 0.2, ...]'
     */
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Embed text and return directly as pgvector-compatible string.
     */
    public String embedAsVectorString(String text) {
        return toVectorString(embed(text));
    }

    private float[] parseEmbedding(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode values = root.path("embedding").path("values");

            if (values.isMissingNode() || !values.isArray()) {
                throw new RuntimeException("Invalid embedding response: missing 'embedding.values'");
            }

            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = (float) values.get(i).asDouble();
            }

            if (embedding.length != EXPECTED_DIMENSIONS) {
                log.warn("Expected {} dimensions but got {}", EXPECTED_DIMENSIONS, embedding.length);
            }

            return embedding;
        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", response, e);
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }
}
