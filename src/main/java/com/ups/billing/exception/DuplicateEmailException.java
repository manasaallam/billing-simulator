package com.ups.billing.exception;

/**
 * Thrown when a signup is attempted with an email that already exists. Mapped to
 * HTTP 409 Conflict.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
