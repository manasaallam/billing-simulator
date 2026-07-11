package com.example.billingsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroqResponse {

    private List<Choice> choices;

    @Data
    public static class Choice {

        private Message message;
    }

    @Data
    public static class Message {

        private String content;
    }
}
