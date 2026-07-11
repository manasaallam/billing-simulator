package com.example.billingsimulator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "groq")
@Data
@Component
public class GroqProperties {

    private Api api;
    private String model;

    @Data
    public static class Api {
        private String key;
    }
}