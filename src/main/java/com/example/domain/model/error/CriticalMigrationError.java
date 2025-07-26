package com.example.domain.model.error;

public class CriticalMigrationError extends MigrationError {

    public CriticalMigrationError(String message) {
        super(message);
    }

    @Override
    public Type getType() {
        return Type.CRITICAL;
    }
}