package com.example.billingsimulator.model;

import java.util.List;

/**
 * A specific clarification question sent back to the customer when
 * extracted parameters are ambiguous or incomplete.
 *
 * Designed to be rendered in the React UI as a follow-up prompt with
 * clickable options (or free-text input).
 *
 * Examples:
 *   { field: "fromService",
 *     question: "Which service level are you shifting shipments FROM?",
 *     suggestedOptions: ["Ground", "Express", "Next Day Air", ...] }
 *
 *   { field: "changeValue",
 *     question: "A 10000% increase seems very large. Did you mean 10000 packages instead?",
 *     suggestedOptions: ["10000%", "10000 packages"] }
 */
public class ClarificationQuestion {

    private String field;                   // Which parameter field needs clarification
    private String question;                // Human-readable question to display
    private List<String> suggestedOptions;  // Clickable options (can be empty for free-text)

    public ClarificationQuestion() {}

    public ClarificationQuestion(String field, String question, List<String> suggestedOptions) {
        this.field = field;
        this.question = question;
        this.suggestedOptions = suggestedOptions;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getSuggestedOptions() { return suggestedOptions; }
    public void setSuggestedOptions(List<String> suggestedOptions) { this.suggestedOptions = suggestedOptions; }
}
