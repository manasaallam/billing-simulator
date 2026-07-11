package com.example.billingsimulator.service;

import com.example.billingsimulator.config.GroqProperties;
import com.example.billingsimulator.dto.GroqRequest;
import com.example.billingsimulator.dto.GroqResponse;
import com.example.billingsimulator.dto.PromptBuilder;
import com.example.billingsimulator.dto.SimulationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GroqService {

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final GroqProperties properties;

    public GroqService(RestClient restClient,
                       ObjectMapper mapper,
                       GroqProperties properties) {

        this.restClient = restClient;
        this.mapper = mapper;
        this.properties = properties;
    }

    public SimulationRequest extractParameters(String question)
            throws Exception {

        String prompt = PromptBuilder.build(question);

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

        GroqResponse response =
                restClient.post()
                        .uri("https://api.groq.com/openai/v1/chat/completions")
                        .header("Authorization",
                                "Bearer " + properties.getApi().getKey())
                        .header("Content-Type",
                                "application/json")
                        .body(request)
                        .retrieve()
                        .body(GroqResponse.class);

        String json =
                response.getChoices()
                        .get(0)
                        .getMessage()
                        .getContent();

        json = json.replace("```json", "")
                .replace("```", "")
                .trim();

        return mapper.readValue(json, SimulationRequest.class);
    }
}