# 📡 Real-Time Migration Updates with Server-Sent Events (SSE)

## 🔍 What is SSE?

**Server-Sent Events (SSE)**  is a way for servers to push real-time updates to clients over HTTP.

Unlike WebSockets, which open a **two-way channel** (requiring “keep-alive” mechanism from the client side), SSE is **one-way** — the server just streams events when something changes.
Perfect for progress bars, notifications, or logs — cases where the user only needs to **see updates** instead of constantly sending data back.

---

- ✅ **Real-time progress** – as the job runs, `MigrationSession` reports progress, and SSE streams those updates to the client.
- ✅ **Cancelable jobs** – a `/cancel` endpoint stops the migration and triggers resource cleanup.
- ✅ **Encapsulation with `MigrationSession`** – every migration is wrapped in a `MigrationSession`, which takes care of setup and cleanup.
    - **Uses `try-with-resources`** to automatically close and clean up resources when the migration finishes.
    - **Submits to a single-thread executor** so heavy jobs run one at a time, avoiding overload.

```java
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
```