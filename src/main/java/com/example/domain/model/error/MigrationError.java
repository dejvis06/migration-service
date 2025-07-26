package com.example.domain.model.error;

public abstract class MigrationError {

    public enum Type {
        WARNING,
        VALIDATION,
        PERMISSION,
        CRITICAL
    }

    private final String message;

    protected MigrationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public abstract Type getType();
}
