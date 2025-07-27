package com.example.infrastructure.migrations;

import com.example.domain.model.entity.User;
import com.example.domain.model.error.CriticalMigrationError;
import com.example.domain.model.error.WarningMigrationError;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserMigrationTest {

    @Test
    void testRunProcessesCsvAndPopulatesUsers() throws Exception {
        // ✅ Run migration on the CSV in test/resources
        UserMigration migration = new UserMigration();
        migration.run();

        // ✅ Migration should be inactive when finished
        assertFalse(migration.isActive());

        // ✅ No critical errors should appear
        boolean hasCriticalError = migration.getErrors().stream()
                .anyMatch(err -> err instanceof CriticalMigrationError);
        assertFalse(hasCriticalError, "Expected no critical errors");

        // ✅ Extract private 'users' list via reflection
        Field usersField = UserMigration.class.getDeclaredField("users");
        usersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<User> users = (List<User>) usersField.get(migration);

        // ✅ Verify all 11 rows (minus header) were processed
        assertEquals(11, users.size());
        assertEquals("John Doe", users.get(0).getName());
        assertEquals("ADMIN", users.get(0).getRole().name());
        assertEquals("Yo Yo", users.get(10).getName());
    }

    @Test
    void testShutdownClosesReaders() throws Exception {
        UserMigration migration = new UserMigration();

        // ✅ Access the private reader fields before shutdown
        Field readerField = UserMigration.class.getDeclaredField("reader");
        Field counterField = UserMigration.class.getDeclaredField("counter");
        readerField.setAccessible(true);
        counterField.setAccessible(true);

        assertNotNull(readerField.get(migration));
        assertNotNull(counterField.get(migration));

        // ✅ Call shutdown
        migration.shutdown();

        // ✅ Readers should be null after shutdown
        assertNull(readerField.get(migration));
        assertNull(counterField.get(migration));
    }

    @Test
    void testProcessLineAddsWarningForInvalidLine() throws Exception {
        UserMigration migration = new UserMigration();

        // ✅ Access private processLine method
        var processLine = UserMigration.class.getDeclaredMethod("processLine", String.class);
        processLine.setAccessible(true);

        // ✅ Invoke with an invalid CSV line
        processLine.invoke(migration, "broken_line");

        assertTrue(migration.getErrors().stream()
                .anyMatch(err -> err instanceof WarningMigrationError));
    }
}

