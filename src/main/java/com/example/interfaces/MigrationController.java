package com.example.interfaces;

import com.example.infrastructure.MigrationSession;
import com.example.infrastructure.migrations.UserMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/migration")
public class MigrationController {

    private static final Logger log = LoggerFactory.getLogger(MigrationController.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private MigrationSession currentSession;

    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter();

        CompletableFuture.runAsync(() -> {
            try (MigrationSession session = new MigrationSession(new UserMigration(), executorService)) {
                log.info("Migration session started for client.");

                this.currentSession = session;

                while (true) {
                    try {
                        Thread.sleep(1000); // ✅ simulate migration work

                        if (!session.isActive()) {
                            break; // ✅ exit immediately, don’t send again
                        }

                        double percentage = session.getProgressPercentage();
                        emitter.send(percentage);   // ✅ flush each update
                        log.info("Sent update {} to client.", percentage);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Migration worker interrupted");
                        break;
                    } catch (IOException e) {
                        log.error("Client disconnected or send failed", e);
                        break;
                    }
                }

                emitter.send(-1); // ✅ signal finished
                emitter.complete();
                log.info("Migration session complete.");
            } catch (IOException e) {
                log.error("Failed to send completion event", e);
                emitter.completeWithError(e);
            }
            this.currentSession = null;
        });

        return emitter;
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelMigration() {
        if (currentSession != null) {
            currentSession.close();   // ✅ stops the worker & cleans up
            currentSession = null;
            return ResponseEntity.ok("Migration canceled.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("No active migration to cancel.");
    }
}
