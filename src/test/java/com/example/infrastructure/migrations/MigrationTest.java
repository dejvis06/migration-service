package com.example.infrastructure.migrations;

import com.example.domain.model.error.MigrationError;
import com.example.domain.model.error.WarningMigrationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {

    // ✅ A simple test subclass to simulate a migration
    static class TestMigration extends Migration {
        @Override
        public void run() {
            // simulate work
            setPercentage(5, 10);
        }

        @Override
        public void shutdown() {
            active = false;
        }
    }

    @Test
    void testSetAndGetProgressPercentage() {
        TestMigration migration = new TestMigration();

        // run migration (sets percentage)
        migration.run();

        assertEquals(50.0, migration.getPercentage());
    }

    @Test
    void testAddErrors() {
        TestMigration migration = new TestMigration();
        migration.getErrors().add(new WarningMigrationError("Test error") {
        });

        List<MigrationError> errors = migration.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Test error", errors.getFirst().getMessage());
    }

    @Test
    void testShutdownChangesActiveFlag() {
        TestMigration migration = new TestMigration();

        assertTrue(migration.isActive());  // should start active
        migration.shutdown();
        assertFalse(migration.isActive()); // should be inactive after shutdown
    }
}
