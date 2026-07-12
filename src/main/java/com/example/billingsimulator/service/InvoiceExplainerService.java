package com.example.billingsimulator.service;

import com.example.billingsimulator.model.InvoiceExplanation;
import com.example.billingsimulator.model.InvoiceQuery;
import com.example.billingsimulator.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Smart Invoice Explainer — answers customer questions about their invoices.
 *
 * Flow:
 *   1. Customer asks a question
 *   2. InvoiceKnowledgeService retrieves relevant context (surcharge rules, fuel schedule, etc.)
 *   3. AiClient (Gemini on hackathon day) generates a business-friendly explanation
 *   4. Returns structured InvoiceExplanation
 */
@Service
public class InvoiceExplainerService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExplainerService.class);

    private final AiClient aiClient;
    private final InvoiceKnowledgeService knowledgeService;

    private static final String SYSTEM_PROMPT = """
            You are a UPS billing expert assistant. A customer is asking about their invoice charges.
            Use the provided knowledge context to give a clear, concise, business-friendly explanation.

            Rules:
            - Explain charges in simple language, not technical jargon
            - Reference specific rates and rules from the context when available
            - If the context doesn't contain enough info, say so honestly
            - Keep explanations under 3-4 sentences unless the question is complex
            - Include the specific dollar amounts or percentages when known
            """;

    public InvoiceExplainerService(AiClient aiClient, InvoiceKnowledgeService knowledgeService) {
        this.aiClient = aiClient;
        this.knowledgeService = knowledgeService;
    }

    public InvoiceExplanation explain(InvoiceQuery query) {
        log.info("Explaining invoice query for customer: {}", query.getCustomerId());

        // Step 1: Retrieve relevant knowledge
        String knowledgeContext = knowledgeService.searchRelevantKnowledge(query.getQuestion());
        log.info("Retrieved knowledge context ({} chars)", knowledgeContext.length());

        // Step 2: Build prompt with context
        String userPrompt = buildUserPrompt(query, knowledgeContext);

        // Step 3: Get AI explanation
        String aiResponse = aiClient.generate(SYSTEM_PROMPT, userPrompt);

        // Step 4: Build response
        InvoiceExplanation explanation = new InvoiceExplanation();
        explanation.setExplanation(aiResponse);
        explanation.setSources(List.of("UPS Surcharge Rules", "Fuel Schedule 2026", "Contract Terms"));
        explanation.setConfidenceLevel(knowledgeContext.contains("SURCHARGE RULE") ? "HIGH" : "MEDIUM");

        return explanation;
    }

    private String buildUserPrompt(InvoiceQuery query, String knowledgeContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("KNOWLEDGE CONTEXT:\n");
        prompt.append(knowledgeContext);
        prompt.append("\nCUSTOMER QUESTION:\n");
        prompt.append(query.getQuestion());

        if (query.getInvoiceId() != null) {
            prompt.append("\nInvoice ID: ").append(query.getInvoiceId());
        }
        if (query.getTrackingNumber() != null) {
            prompt.append("\nTracking: ").append(query.getTrackingNumber());
        }

        return prompt.toString();
    }
}
