package com.example.billingsimulator.dto;

public final class FollowUpPromptBuilder {

    private FollowUpPromptBuilder() {
    }

    public static String build(String previousQuestion,
                               String followUpAnswer) {

        return PromptBuilder.build(
                """
                Previous customer request:
                %s

                Customer follow-up:
                %s

                Update the previous customer request using the follow-up answer.

                Return the final simulation request.
                """
                        .formatted(previousQuestion, followUpAnswer)
        );
    }
}