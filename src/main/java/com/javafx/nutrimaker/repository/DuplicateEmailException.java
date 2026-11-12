package com.javafx.nutrimaker.repository;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
