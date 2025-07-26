package com.example.infrastructure.migrations;

import com.example.domain.model.error.MigrationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Migration implements Runnable {

    protected boolean active = true;
    protected double percentage = 0.0;
    protected final List<MigrationError> errors = Collections.synchronizedList(new ArrayList<>());

    public double getProgressPercentage() {
        return percentage;
    }

    protected void setPercentage(int processedLines, int totalLines) {
        percentage = (processedLines * 100.0) / totalLines;
    }

    public List<MigrationError> getErrors() {
        return errors;
    }

    public abstract void shutdown();

    public boolean isActive() {
        return active;
    }
}