package com.example.application;

import com.example.domain.model.entity.User;
import com.example.infrastructure.MigrationSession;
import com.example.infrastructure.migrations.UserMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    private MigrationSession currentSession;

    public MigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void run(SseEmitter emitter) {
        try (MigrationSession session = new MigrationSession(new UserMigration())) {

            this.currentSession = session;
            log.info("Migration session started for client.");

            UserMigration migration = (UserMigration) session.getMigration();

            while (true) {
                try {
                    //Thread.sleep(1000); // ✅ simulate migration work

                    if (!session.isActive()) {
                        break; // ✅ exit immediately, don’t send again
                    }

                    double percentage = session.getProgressPercentage();
                    emitter.send(percentage);   // ✅ flush each update
                    log.info("Sent update {} to client.", percentage);

                    if (migration.isBatchReady()) {
                        // ✅ Retrieve and clear the batch
                        List<User> batch = migration.drainBatch();
                        // Persist batch
                        log.info("Persisting batch.");
                    }

                } /* catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Migration worker interrupted");
                    break;
                }*/ catch (IOException e) {
                    log.error("Client disconnected or send failed", e);
                    break;
                }
            }

            // ✅ Process any leftover users after file is fully read
            List<User> remaining = migration.getRemainingUsers();
            if (!remaining.isEmpty()) {
                // Persist batch
                log.info("Persisting remaining batch.");
            }

            emitter.send(-1); // ✅ signal finished
            emitter.complete();
            log.info("Migration session complete.");
        } catch (IOException e) {
            log.error("Failed to send completion event", e);
            emitter.completeWithError(e);
        }
    }

    public boolean cancel() {
        if (currentSession != null) {
            currentSession.close();
            currentSession = null;
            return true;
        }
        return false;
    }
}

