package com.example.billingsimulator.exception;

/** Thrown when login credentials are invalid. → HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) { super(message); }
}
