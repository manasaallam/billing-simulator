package com.example.billingsimulator.service;

import com.example.billingsimulator.model.KnowledgeArticle;
import com.example.billingsimulator.repository.KnowledgeArticleRepository;
import com.example.billingsimulator.service.ai.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads knowledge JSON files, chunks each entry into text,
 * embeds via Gemini text-embedding-004, and stores into knowledge_article table.
 *
 * Activated only when gemini.api-key is set (needs EmbeddingService).
 *
 * Usage:
 *   - Call ingestAll() to load all 3 knowledge files
 *   - Skips entries that already exist (upsert by company_id + kind + key_code)
 *   - Call reindexAll() to clear and re-ingest everything
 */
@Service
@ConditionalOnProperty(name = "gemini.api-key")
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final KnowledgeArticleRepository repository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeIngestionService(KnowledgeArticleRepository repository,
                                     EmbeddingService embeddingService,
                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingest all knowledge files. Skips if embeddings already exist.
     */
    @Transactional
    public void ingestAll() {
        long existing = repository.countByCompanyIdIsNull();
        if (existing > 0) {
            log.info("Knowledge base already has {} global entries, skipping ingestion", existing);
            return;
        }

        log.info("Starting knowledge ingestion...");
        List<KnowledgeArticle> articles = new ArrayList<>();

        articles.addAll(chunkSurchargeRules());
        articles.addAll(chunkChargeExplanations());
        articles.addAll(chunkFuelSchedule());

        log.info("Embedding and storing {} knowledge chunks...", articles.size());

        for (KnowledgeArticle article : articles) {
            String vectorString = embeddingService.embedAsVectorString(article.getContent());
            article.setEmbedding(vectorString);
            article.setTokenCount(estimateTokenCount(article.getContent()));
        }

        repository.saveAll(articles);
        log.info("Knowledge ingestion complete: {} articles stored", articles.size());
    }

    /**
     * Clear all global knowledge and re-ingest from scratch.
     */
    @Transactional
    public void reindexAll() {
        log.info("Clearing all global knowledge articles...");
        repository.deleteAllGlobal();
        
        log.info("Re-ingesting knowledge...");
        List<KnowledgeArticle> articles = new ArrayList<>();
        articles.addAll(chunkSurchargeRules());
        articles.addAll(chunkChargeExplanations());
        articles.addAll(chunkFuelSchedule());

        for (KnowledgeArticle article : articles) {
            String vectorString = embeddingService.embedAsVectorString(article.getContent());
            article.setEmbedding(vectorString);
            article.setTokenCount(estimateTokenCount(article.getContent()));
        }

        repository.saveAll(articles);
        log.info("Re-indexing complete: {} articles stored", articles.size());
    }

    // ── Chunking: surcharge-rules.json ────────────────────────────

    private List<KnowledgeArticle> chunkSurchargeRules() {
        List<Map<String, Object>> rules = loadJson("knowledge/surcharge-rules.json");
        List<KnowledgeArticle> articles = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            String code = (String) rule.get("code");
            String name = (String) rule.get("name");

            String content = String.format(
                    "Surcharge: %s. Code: %s. Description: %s. " +
                    "Current Rate: %s. Trigger Rule: %s. Business Reason: %s.",
                    name, code,
                    rule.getOrDefault("description", ""),
                    rule.getOrDefault("currentRate", ""),
                    rule.getOrDefault("triggerRule", ""),
                    rule.getOrDefault("businessReason", "")
            );

            KnowledgeArticle article = new KnowledgeArticle();
            article.setKind("SURCHARGE");
            article.setKeyCode(code);
            article.setContent(content);
            article.setBusinessReason((String) rule.getOrDefault("businessReason", null));
            article.setSourceDoc("surcharge-rules.json");
            articles.add(article);
        }

        log.info("Chunked {} surcharge rules", articles.size());
        return articles;
    }

    // ── Chunking: charge-explanations.json ────────────────────────

    private List<KnowledgeArticle> chunkChargeExplanations() {
        List<Map<String, Object>> explanations = loadJson("knowledge/charge-explanations.json");
        List<KnowledgeArticle> articles = new ArrayList<>();

        for (Map<String, Object> explanation : explanations) {
            String section = (String) explanation.get("section");
            String keyCode = section.toUpperCase().replace(" ", "_");

            String content = String.format(
                    "Invoice Section: %s. Meaning: %s. Details: %s.",
                    section,
                    explanation.getOrDefault("meaning", ""),
                    explanation.getOrDefault("details", "")
            );

            KnowledgeArticle article = new KnowledgeArticle();
            article.setKind("CHARGE_EXPLANATION");
            article.setKeyCode(keyCode);
            article.setContent(content);
            article.setSourceDoc("charge-explanations.json");
            articles.add(article);
        }

        log.info("Chunked {} charge explanations", articles.size());
        return articles;
    }

    // ── Chunking: fuel-schedule.json ──────────────────────────────

    private List<KnowledgeArticle> chunkFuelSchedule() {
        List<Map<String, Object>> weeks = loadJson("knowledge/fuel-schedule.json");
        List<KnowledgeArticle> articles = new ArrayList<>();

        // Create one summary chunk for the full fuel schedule
        StringBuilder summary = new StringBuilder("Fuel Surcharge Schedule (recent weeks):\n");
        for (Map<String, Object> week : weeks) {
            summary.append(String.format(
                    "Week %s: Diesel Index = %s, Ground Fuel Surcharge = %s%%, Air Fuel Surcharge = %s%%.\n",
                    week.get("week"),
                    week.get("dieselIndex"),
                    week.get("groundFuelPct"),
                    week.get("airFuelPct")
            ));
        }
        summary.append("Fuel surcharge percentages are updated weekly based on the national diesel price index.");

        KnowledgeArticle article = new KnowledgeArticle();
        article.setKind("FUEL_SCHEDULE");
        article.setKeyCode("WEEKLY_SUMMARY");
        article.setContent(summary.toString());
        article.setBusinessReason("Fuel surcharges offset carrier fuel costs and are adjusted weekly.");
        article.setSourceDoc("fuel-schedule.json");
        articles.add(article);

        log.info("Chunked fuel schedule into 1 summary article");
        return articles;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private List<Map<String, Object>> loadJson(String path) {
        try {
            InputStream is = new ClassPathResource(path).getInputStream();
            return objectMapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to load knowledge file: {}", path, e);
            return List.of();
        }
    }

    private int estimateTokenCount(String text) {
        // Rough estimate: ~4 chars per token for English text
        return text.length() / 4;
    }
}
