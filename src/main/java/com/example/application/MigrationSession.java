package com.example.application;

import com.example.domain.model.error.MigrationError;
import com.example.infrastructure.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MigrationSession implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MigrationSession.class);

    private final Migration migration;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MigrationSession(Migration migration) {
        this.migration = migration;

        log.info("Submitting migration worker to ExecutorService");
        executor.submit(migration);
    }

    public double getProgressPercentage() {
        return migration.getProgressPercentage();
    }

    public boolean isActive() {
        return migration.isActive();
    }

    public List<MigrationError> getErrors() {
        return migration.getErrors();
    }

    @Override
    public void close() {
        log.info("Closing MigrationSession");
        migration.shutdown();
    }
}