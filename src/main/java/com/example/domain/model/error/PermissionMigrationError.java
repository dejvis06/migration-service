package com.example.domain.model.error;

public class PermissionMigrationError extends MigrationError {

    public PermissionMigrationError(String message) {
        super(message);
    }

    @Override
    public Type getType() {
        return Type.PERMISSION;
    }
}