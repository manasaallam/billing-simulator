package com.example.billingsimulator.service;

import com.example.billingsimulator.config.GroqProperties;
import com.example.billingsimulator.dto.*;
import com.example.billingsimulator.util.FollowUpDetector;
import com.example.billingsimulator.util.SimulationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GroqService {

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final GroqProperties properties;
    private final SimulationValidator simulationValidator;
    private final ConversationService conversationService;

    public GroqService(RestClient restClient,
                       ObjectMapper mapper,
                       GroqProperties properties,
                       SimulationValidator simulationValidator,
                       ConversationService conversationService) {

        this.restClient = restClient;
        this.mapper = mapper;
        this.properties = properties;
        this.simulationValidator = simulationValidator;
        this.conversationService = conversationService;
    }

    public AIResponse extractParameters(String conversationId,
                                        String question)
            throws Exception {

        if (!conversationService.exists(conversationId)
                && FollowUpDetector.isFollowUpAnswer(question)) {

            AIResponse response = new AIResponse();
            response.setStatus("NO_ACTIVE_CONVERSATION");
            response.setMessage(
                    "There is no active simulation. Please start a new billing simulation request."
            );

            return response;
        }

        String prompt;

        if (conversationService.exists(conversationId)) {

            String previousQuestion = conversationService.get(conversationId);

            prompt = FollowUpPromptBuilder.build(previousQuestion, question);

        } else {

            prompt = PromptBuilder.build(question);
        }
        GroqRequest request = new GroqRequest(
                properties.getModel(),
                List.of(
                        new GroqRequest.Message(
                                "user",
                                prompt
                        )
                ),
                0.1
        );

        GroqResponse response = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.getApi().getKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(GroqResponse.class);

        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Empty response received from Groq.");
        }

        String content = response.getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        System.out.println("==================================");
        System.out.println(content);
        System.out.println("==================================");
        AIResponse aiResponse = mapper.readValue(content, AIResponse.class);
        aiResponse =
                simulationValidator.validate(question, aiResponse);

        System.out.println("====================================");
        System.out.println("Conversation ID : " + conversationId);
        System.out.println("Exists before processing : "
                + conversationService.exists(conversationId));
        System.out.println("====================================");

        if ("NEEDS_MORE_INFORMATION".equals(aiResponse.getStatus())) {

            String conversationText;

            if (conversationService.exists(conversationId)) {

                conversationText =
                        conversationService.get(conversationId) +
                                "\n" +
                                question;

            } else {

                conversationText = question;
            }

            conversationService.save(conversationId, conversationText);
            System.out.println("Conversation Saved:");
            System.out.println(conversationService.get(conversationId));

        } else {

            System.out.println("Removing conversation : " + conversationId);
            conversationService.remove(conversationId);
            System.out.println("Exists after remove : "
                    + conversationService.exists(conversationId));
        }

        return aiResponse;
    }
}