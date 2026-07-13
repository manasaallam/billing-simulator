package com.example.billingsimulator.exception;

/** Thrown when signup is attempted with an email that already exists. → HTTP 409. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) { super(message); }
}
