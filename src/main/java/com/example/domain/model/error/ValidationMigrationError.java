package com.example.domain.model.error;

public class ValidationMigrationError extends MigrationError {

    public ValidationMigrationError(String message) {
        super(message);
    }

    @Override
    public Type getType() {
        return Type.VALIDATION;
    }
}