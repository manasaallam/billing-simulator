package com.example.billingsimulator.model;

import java.util.List;

/**
 * API response returned to the React UI.
 *
 * Three possible states:
 *   SUCCESS              → result is populated, clarificationQuestions is null
 *   NEEDS_CLARIFICATION  → clarificationQuestions populated, result is null
 *   ERROR                → errorMessage populated
 *
 * Example (success):
 *   { "status": "SUCCESS",
 *     "result": { "currentCost": 502000, "projectedCost": 449000, "savings": 53000, ... },
 *     "disclaimer": "These results are projections..." }
 *
 * Example (needs clarification):
 *   { "status": "NEEDS_CLARIFICATION",
 *     "conversationId": "abc-123",
 *     "clarificationQuestions": [{ "field": "fromService", "question": "...", ... }] }
 */
public class SimulationResponse {

    private String status;          // SUCCESS, NEEDS_CLARIFICATION, ERROR
    private SimulationResult result;
    private List<ClarificationQuestion> clarificationQuestions;
    private String conversationId;
    private String disclaimer;
    private String errorMessage;

    public SimulationResponse() {}

    public static SimulationResponse success(SimulationResult result, String conversationId) {
        SimulationResponse response = new SimulationResponse();
        response.setStatus("SUCCESS");
        response.setResult(result);
        response.setConversationId(conversationId);
        response.setDisclaimer("These results are projections based on historical data and are not final quotes.");
        return response;
    }

    public static SimulationResponse needsClarification(List<ClarificationQuestion> questions, String conversationId) {
        SimulationResponse response = new SimulationResponse();
        response.setStatus("NEEDS_CLARIFICATION");
        response.setClarificationQuestions(questions);
        response.setConversationId(conversationId);
        return response;
    }

    public static SimulationResponse error(String message) {
        SimulationResponse response = new SimulationResponse();
        response.setStatus("ERROR");
        response.setErrorMessage(message);
        return response;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public SimulationResult getResult() { return result; }
    public void setResult(SimulationResult result) { this.result = result; }

    public List<ClarificationQuestion> getClarificationQuestions() { return clarificationQuestions; }
    public void setClarificationQuestions(List<ClarificationQuestion> clarificationQuestions) { this.clarificationQuestions = clarificationQuestions; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
