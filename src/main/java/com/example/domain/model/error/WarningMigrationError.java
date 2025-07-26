package com.example.domain.model.error;

public class WarningMigrationError extends MigrationError {

    public WarningMigrationError(String message) {
        super(message);
    }

    @Override
    public Type getType() {
        return Type.WARNING;
    }
}
