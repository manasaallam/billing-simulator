package com.ups.billing.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard error payload returned by the API:
 * {@code { "message": "...", "fieldErrors": { "field": "msg" } } }.
 */
public class ApiError {

    private final String message;
    private final Map<String, String> fieldErrors;

    public ApiError(String message) {
        this(message, new HashMap<>());
    }

    public ApiError(String message, Map<String, String> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
