package com.example.application;
import com.example.application.MigrationService;
import com.example.infrastructure.MigrationSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MigrationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private MigrationService migrationService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        migrationService = new MigrationService(jdbcTemplate);
    }

    @Test
    void cancel_returnsTrueAndClosesSession() {
        // given
        MigrationSession session = mock(MigrationSession.class);
        setCurrentSession(migrationService, session);

        // when
        boolean canceled = migrationService.cancel();

        // then
        assertTrue(canceled);
        verify(session).close();
        assertNull(getCurrentSession(migrationService));
    }

    @Test
    void cancel_returnsFalseIfNoActiveSession() {
        // when
        boolean canceled = migrationService.cancel();

        // then
        assertFalse(canceled);
    }

    @Test
    void run_completesEmitter() throws Exception {
        // ⚠️ This test will run the method, but we won't mock MigrationSession —
        // we just check SSE doesn't explode and completes cleanly
        SseEmitter emitter = mock(SseEmitter.class);

        // act
        migrationService.run(emitter);

        // assert: we expect that complete() is eventually called
        verify(emitter).complete();
    }

    // --- helper methods for private field access ---

    private void setCurrentSession(MigrationService service, MigrationSession session) {
        try {
            var field = MigrationService.class.getDeclaredField("currentSession");
            field.setAccessible(true);
            field.set(service, session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MigrationSession getCurrentSession(MigrationService service) {
        try {
            var field = MigrationService.class.getDeclaredField("currentSession");
            field.setAccessible(true);
            return (MigrationSession) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


