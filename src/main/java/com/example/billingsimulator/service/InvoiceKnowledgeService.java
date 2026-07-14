package com.example.billingsimulator.service;

import com.example.billingsimulator.repository.KnowledgeArticleRepository;
import com.example.billingsimulator.service.ai.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and searches invoice knowledge.
 *
 * Two modes:
 *   1. Vector search (RAG) — when EmbeddingService + KnowledgeArticleRepository are available
 *      Embeds the user question, runs cosine similarity against knowledge_article table.
 *   2. Keyword search (fallback) — when no Gemini API key / no DB connection
 *      Loads JSON files into memory, matches by keyword.
 *
 * Knowledge sources:
 *   - surcharge-rules.json    → what each surcharge is, when it triggers, current rate
 *   - charge-explanations.json → what each invoice section means
 *   - fuel-schedule.json       → weekly diesel index → fuel surcharge %
 */
@Service
public class InvoiceKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceKnowledgeService.class);
    private static final int TOP_K = 5;

    private final ObjectMapper objectMapper;

    // Optional — only injected when gemini.api-key is set and DB is connected
    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private KnowledgeArticleRepository knowledgeArticleRepository;

    private List<Map<String, Object>> surchargeRules = new ArrayList<>();
    private List<Map<String, Object>> chargeExplanations = new ArrayList<>();
    private List<Map<String, Object>> fuelSchedule = new ArrayList<>();

    public InvoiceKnowledgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Precomputed embeddings for in-memory vector search (no DB needed)
    private List<Map<String, Object>> precomputedEmbeddings = new ArrayList<>();

    @PostConstruct
    public void loadKnowledge() {
        surchargeRules = loadJson("knowledge/surcharge-rules.json");
        chargeExplanations = loadJson("knowledge/charge-explanations.json");
        fuelSchedule = loadJson("knowledge/fuel-schedule.json");
        precomputedEmbeddings = loadJson("knowledge/precomputed-embeddings.json");
        log.info("Loaded knowledge: {} surcharge rules, {} charge explanations, {} fuel weeks, {} precomputed embeddings",
                surchargeRules.size(), chargeExplanations.size(), fuelSchedule.size(), precomputedEmbeddings.size());
    }

    /**
     * Search knowledge sources for content relevant to the customer's question.
     *
     * Priority:
     *   1. In-memory vector search (precomputed embeddings + Gemini for query embedding)
     *   2. DB vector search (pgvector)
     *   3. Keyword-based fallback
     */
    public String searchRelevantKnowledge(String question) {
        if (isInMemoryVectorSearchAvailable()) {
            return inMemoryVectorSearch(question);
        }
        if (isDbVectorSearchAvailable()) {
            return dbVectorSearch(question);
        }
        return keywordSearch(question);
    }

    private boolean isInMemoryVectorSearchAvailable() {
        return embeddingService != null && !precomputedEmbeddings.isEmpty();
    }

    private boolean isDbVectorSearchAvailable() {
        return embeddingService != null && knowledgeArticleRepository != null
                && knowledgeArticleRepository.countByCompanyIdIsNull() > 0;
    }

    /**
     * In-memory RAG: Embed the question, compute cosine similarity against precomputed vectors.
     */
    private String inMemoryVectorSearch(String question) {
        try {
            float[] queryEmbedding = embeddingService.embed(question);

            // Compute similarity for each precomputed embedding
            List<Map.Entry<Double, String>> scored = new ArrayList<>();
            for (Map<String, Object> article : precomputedEmbeddings) {
                List<Number> embeddingValues = (List<Number>) article.get("embedding");
                if (embeddingValues == null || embeddingValues.isEmpty()) continue;

                float[] docEmbedding = new float[embeddingValues.size()];
                for (int i = 0; i < embeddingValues.size(); i++) {
                    docEmbedding[i] = embeddingValues.get(i).floatValue();
                }

                double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                String content = (String) article.get("content");
                String kind = (String) article.get("kind");
                scored.add(Map.entry(similarity, kind + ": " + content));
            }

            // Sort by similarity descending, take top-K
            scored.sort((a, b) -> Double.compare(b.getKey(), a.getKey()));

            StringBuilder context = new StringBuilder();
            int count = 0;
            for (Map.Entry<Double, String> entry : scored) {
                if (count >= TOP_K) break;
                if (entry.getKey() < 0.3) break;

                context.append(entry.getValue()).append("\n\n");
                count++;
            }

            if (context.isEmpty()) {
                log.info("In-memory vector search found no relevant results, falling back to keyword");
                return keywordSearch(question);
            }

            log.info("In-memory vector search returned {} relevant chunks (top similarity: {})",
                    count, scored.get(0).getKey());
            return context.toString();
        } catch (Exception e) {
            log.warn("In-memory vector search failed, falling back to keyword: {}", e.getMessage());
            return keywordSearch(question);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dotProduct / denom;
    }

    /**
     * DB RAG: Embed the question, query pgvector for top-K similar knowledge chunks.
     */
    private String dbVectorSearch(String question) {
        try {
            String queryVector = embeddingService.embedAsVectorString(question);
            List<Object[]> results = knowledgeArticleRepository.findSimilarGlobal(queryVector, TOP_K);

            if (results.isEmpty()) {
                log.info("Vector search returned no results, falling back to keyword search");
                return keywordSearch(question);
            }

            StringBuilder context = new StringBuilder();
            for (Object[] row : results) {
                String content = (String) row[1];
                String kind = (String) row[2];
                double similarity = ((Number) row[5]).doubleValue();

                if (similarity < 0.3) continue; // skip low-relevance results

                context.append(kind).append(": ").append(content).append("\n\n");
            }

            if (context.isEmpty()) {
                return keywordSearch(question);
            }

            log.info("Vector search returned {} relevant chunks", results.size());
            return context.toString();
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to keyword search: {}", e.getMessage());
            return keywordSearch(question);
        }
    }

    /**
     * Fallback: Keyword-based search over in-memory JSON knowledge.
     */
    private String keywordSearch(String question) {
        String lowerQuestion = question.toLowerCase();
        StringBuilder context = new StringBuilder();

        // Search surcharge rules
        for (Map<String, Object> rule : surchargeRules) {
            String name = ((String) rule.get("name")).toLowerCase();
            String code = ((String) rule.get("code")).toLowerCase();
            if (lowerQuestion.contains(name) || lowerQuestion.contains(code)
                    || matchesKeywords(lowerQuestion, name)) {
                context.append("SURCHARGE RULE: ").append(formatEntry(rule)).append("\n\n");
            }
        }

        // Search charge explanations
        for (Map<String, Object> explanation : chargeExplanations) {
            String section = ((String) explanation.get("section")).toLowerCase();
            if (lowerQuestion.contains(section) || matchesKeywords(lowerQuestion, section)) {
                context.append("CHARGE INFO: ").append(formatEntry(explanation)).append("\n\n");
            }
        }

        // Fuel-related questions get the schedule
        if (lowerQuestion.contains("fuel") || lowerQuestion.contains("diesel")
                || lowerQuestion.contains("surcharge")) {
            context.append("FUEL SCHEDULE (recent weeks):\n");
            // Show last 4 weeks
            int start = Math.max(0, fuelSchedule.size() - 4);
            for (int i = start; i < fuelSchedule.size(); i++) {
                context.append("  ").append(formatEntry(fuelSchedule.get(i))).append("\n");
            }
            context.append("\n");
        }

        // If nothing matched, return general invoice knowledge
        if (context.isEmpty()) {
            context.append("GENERAL INVOICE KNOWLEDGE:\n");
            for (Map<String, Object> explanation : chargeExplanations) {
                context.append("  - ").append(explanation.get("section"))
                        .append(": ").append(explanation.get("meaning")).append("\n");
            }
        }

        return context.toString();
    }

    private boolean matchesKeywords(String question, String target) {
        String[] words = target.split(" ");
        for (String word : words) {
            if (word.length() > 3 && question.contains(word)) return true;
        }
        return false;
    }

    private String formatEntry(Map<String, Object> entry) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : entry.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(" | ");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> loadJson(String path) {
        try {
            InputStream is = new ClassPathResource(path).getInputStream();
            return objectMapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to load knowledge file: {}", path, e);
            return new ArrayList<>();
        }
    }
}
