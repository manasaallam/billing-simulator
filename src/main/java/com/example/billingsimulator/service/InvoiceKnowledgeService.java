package com.example.billingsimulator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and searches invoice knowledge from JSON files.
 * Acts as a simple RAG layer — keyword-based search for now,
 * swappable to Vertex AI Vector Search on hackathon day.
 *
 * Knowledge sources:
 *   - surcharge-rules.json    → what each surcharge is, when it triggers, current rate
 *   - charge-explanations.json → what each invoice section means
 *   - fuel-schedule.json       → weekly diesel index → fuel surcharge %
 */
@Service
public class InvoiceKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceKnowledgeService.class);
    private final ObjectMapper objectMapper;

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
     * Search all knowledge sources for content relevant to the customer's question.
     * Returns matching entries as formatted text for the AI prompt.
     *
     * For hackathon day: replace this with Vector Search embedding + similarity query.
     */
    public String searchRelevantKnowledge(String question) {
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
