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

    @PostConstruct
    public void loadKnowledge() {
        surchargeRules = loadJson("knowledge/surcharge-rules.json");
        chargeExplanations = loadJson("knowledge/charge-explanations.json");
        fuelSchedule = loadJson("knowledge/fuel-schedule.json");
        log.info("Loaded knowledge: {} surcharge rules, {} charge explanations, {} fuel weeks",
                surchargeRules.size(), chargeExplanations.size(), fuelSchedule.size());
    }

    /**
     * Search knowledge sources for content relevant to the customer's question.
     *
     * Uses vector similarity search (RAG) when EmbeddingService + DB are available.
     * Falls back to keyword-based search otherwise.
     */
    public String searchRelevantKnowledge(String question) {
        if (isVectorSearchAvailable()) {
            return vectorSearch(question);
        }
        return keywordSearch(question);
    }

    private boolean isVectorSearchAvailable() {
        return embeddingService != null && knowledgeArticleRepository != null
                && knowledgeArticleRepository.countByCompanyIdIsNull() > 0;
    }

    /**
     * RAG: Embed the question, query pgvector for top-K similar knowledge chunks.
     */
    private String vectorSearch(String question) {
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
