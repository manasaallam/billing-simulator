package com.example.billingsimulator.controller;

import com.example.billingsimulator.service.KnowledgeIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoint to trigger knowledge base ingestion / re-indexing.
 * Only available when Gemini API key is configured (KnowledgeIngestionService is active).
 */
@RestController
@RequestMapping("/api/admin/knowledge")
public class KnowledgeAdminController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAdminController.class);

    @Autowired(required = false)
    private KnowledgeIngestionService ingestionService;

    /**
     * POST /api/admin/knowledge/ingest
     * Ingests knowledge from JSON files. Skips if already ingested.
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingest() {
        if (ingestionService == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Knowledge ingestion unavailable. Set gemini.api-key to enable."));
        }

        ingestionService.ingestAll();
        return ResponseEntity.ok(Map.of("status", "Ingestion complete"));
    }

    /**
     * POST /api/admin/knowledge/reindex
     * Clears all global knowledge and re-ingests from scratch.
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindex() {
        if (ingestionService == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Knowledge ingestion unavailable. Set gemini.api-key to enable."));
        }

        ingestionService.reindexAll();
        return ResponseEntity.ok(Map.of("status", "Re-indexing complete"));
    }
}
