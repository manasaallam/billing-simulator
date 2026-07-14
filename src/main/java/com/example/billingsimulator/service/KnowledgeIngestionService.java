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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private static final int BATCH_SIZE = 10;

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
        articles.addAll(chunkMarkdownDocuments());

        log.info("Embedding and storing {} knowledge chunks in batches of {}...", articles.size(), BATCH_SIZE);

        for (int i = 0; i < articles.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, articles.size());
            List<KnowledgeArticle> batch = articles.subList(i, end);

            for (KnowledgeArticle article : batch) {
                String vectorString = embeddingService.embedAsVectorString(article.getContent());
                article.setEmbedding(vectorString);
                article.setTokenCount(estimateTokenCount(article.getContent()));
            }

            repository.saveAll(batch);
            log.info("Batch {}/{} saved ({} articles)", (i / BATCH_SIZE) + 1,
                    (int) Math.ceil((double) articles.size() / BATCH_SIZE), batch.size());
        }

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
        articles.addAll(chunkMarkdownDocuments());

        for (int i = 0; i < articles.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, articles.size());
            List<KnowledgeArticle> batch = articles.subList(i, end);

            for (KnowledgeArticle article : batch) {
                String vectorString = embeddingService.embedAsVectorString(article.getContent());
                article.setEmbedding(vectorString);
                article.setTokenCount(estimateTokenCount(article.getContent()));
            }

            repository.saveAll(batch);
            log.info("Batch {}/{} saved ({} articles)", (i / BATCH_SIZE) + 1,
                    (int) Math.ceil((double) articles.size() / BATCH_SIZE), batch.size());
        }

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

    // ── Chunking: Markdown documents ────────────────────────────

    private static final String[] MARKDOWN_FILES = {
            "knowledge/Carrier_Services_Agreement_Sample.md",
            "knowledge/Sanitized_Letter_of_Agreement.md",
            "knowledge/Save_as_You_Grow_Pricing_Program.md",
            "knowledge/UPS_Guangdong_Longsys_Letter_of_Agreement.md"
    };

    private List<KnowledgeArticle> chunkMarkdownDocuments() {
        List<KnowledgeArticle> articles = new ArrayList<>();

        for (String filePath : MARKDOWN_FILES) {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            String docKind = deriveKind(fileName);

            try {
                String content = loadText(filePath);
                List<String[]> sections = splitBySections(content);

                int sectionIndex = 0;
                for (String[] section : sections) {
                    String heading = section[0];
                    String body = section[1].trim();

                    if (body.isEmpty() || body.length() < 30) continue;

                    String keyCode = deriveKeyCode(heading, sectionIndex);
                    String chunkText = String.format("Document: %s. Section: %s.\n\n%s",
                            fileName.replace(".md", "").replace("_", " "),
                            heading, body);

                    KnowledgeArticle article = new KnowledgeArticle();
                    article.setKind(docKind);
                    article.setKeyCode(keyCode);
                    article.setContent(chunkText);
                    article.setSourceDoc(fileName);
                    articles.add(article);
                    sectionIndex++;
                }

                log.info("Chunked {} into {} sections", fileName, sectionIndex);
            } catch (Exception e) {
                log.error("Failed to chunk markdown file: {}", filePath, e);
            }
        }

        log.info("Total markdown chunks: {}", articles.size());
        return articles;
    }

    private List<String[]> splitBySections(String markdown) {
        List<String[]> sections = new ArrayList<>();
        String[] lines = markdown.split("\n");
        StringBuilder currentBody = new StringBuilder();
        String currentHeading = "Introduction";

        for (String line : lines) {
            if (line.startsWith("## ") || line.startsWith("# ")) {
                // Save previous section
                if (currentBody.length() > 0) {
                    sections.add(new String[]{currentHeading, currentBody.toString()});
                }
                currentHeading = line.replaceAll("^#+\\s*", "").trim();
                currentBody = new StringBuilder();
            } else {
                currentBody.append(line).append("\n");
            }
        }
        // Save last section
        if (currentBody.length() > 0) {
            sections.add(new String[]{currentHeading, currentBody.toString()});
        }

        return sections;
    }

    private String deriveKind(String fileName) {
        if (fileName.contains("Agreement") || fileName.contains("Letter")) return "CONTRACT";
        if (fileName.contains("Pricing") || fileName.contains("Grow")) return "PRICING_PROGRAM";
        return "DOCUMENT";
    }

    private String deriveKeyCode(String heading, int index) {
        String code = heading.toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "")
                .trim()
                .replace(" ", "_");
        if (code.length() > 50) code = code.substring(0, 50);
        if (code.isEmpty()) code = "SECTION";
        return code + "_" + index;
    }

    private String loadText(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to load text file: {}", path, e);
            return "";
        }
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
