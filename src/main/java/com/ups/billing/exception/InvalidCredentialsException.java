package com.ups.billing.exception;

/**
 * Thrown when login credentials are invalid. Mapped to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
