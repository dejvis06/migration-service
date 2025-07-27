package com.example.infrastructure.migrations;

import com.example.infrastructure.MigrationSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationSessionTest {

    // ✅ simple fake migration for testing
    static class FakeMigration extends Migration {

        @Override
        public void run() {
            // simulate work
            setPercentage(5, 10); // sets progress to 50.0
        }

        @Override
        public void shutdown() {
            active = false;
        }
    }

    @Test
    void testProgressAndActivityDelegation() throws Exception {
        FakeMigration migration = new FakeMigration();

        try (MigrationSession session = new MigrationSession(migration)) {
            // ✅ The migration runs in executor, we just need to give it a moment
            Thread.sleep(100);

            // ✅ Percentage should come from migration
            assertEquals(50.0, session.getProgressPercentage());

            // ✅ Migration should still be active before shutdown
            assertTrue(session.isActive());
        }

        // ✅ After try-with-resources (close called), migration shutdown should have run
        assertFalse(migration.isActive());
    }
}
