package com.example.interfaces;

import com.example.application.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/migration")
public class MigrationController {

    private static final Logger log = LoggerFactory.getLogger(MigrationController.class);

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @GetMapping("/run")
    public SseEmitter run() {
        SseEmitter emitter = new SseEmitter();

        CompletableFuture.runAsync(() -> migrationService.run(emitter));
        return emitter;
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelMigration() {
        boolean canceled = migrationService.cancel();
        if (canceled) {
            return ResponseEntity.ok("Migration canceled.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("No active migration to cancel.");
    }
}
