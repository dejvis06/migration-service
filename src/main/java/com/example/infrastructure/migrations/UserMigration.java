package com.example.infrastructure.migrations;

import com.example.domain.model.entity.Role;
import com.example.domain.model.entity.User;
import com.example.domain.model.error.CriticalMigrationError;
import com.example.domain.model.error.WarningMigrationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class UserMigration extends Migration {

    private static final Logger log = LoggerFactory.getLogger(UserMigration.class);

    private BufferedReader reader;
    private BufferedReader counter;

    private boolean batchReady = false;
    private static final int BATCH_SIZE = 500;
    private final List<User> users = new ArrayList<>();

    public UserMigration() {
        try {
            ClassPathResource resource = new ClassPathResource("migration/users.csv");

            this.reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            this.counter = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        } catch (IOException e) {
            errors.add(new CriticalMigrationError("Failed to open users.csv: " + e.getMessage()));
        }
    }

    @Override
    public void run() {
        try {
            int totalLines = 0;
            while (counter.readLine() != null) {
                totalLines++;
            }
            totalLines = totalLines - 1; // minus header

            String line;
            boolean isHeader = true;
            int processedLines = 0;

            while (reader != null && (line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // skip header line: id,name,email,role
                    continue;
                }
                processLine(line);

                processedLines++;
                setPercentage(processedLines, totalLines);

                // Mark batch ready if it hits size
                if (users.size() == BATCH_SIZE) {
                    batchReady = true;
                    // Pause loop until app layer empties the list
                    synchronized (this) {
                        while (batchReady) {
                            wait();   // wait for signal from app layer
                        }
                    }
                }
                Thread.sleep(1000);
            }
            this.active = false;
            log.info(users.toString());
        } catch (IOException e) {
            errors.add(new CriticalMigrationError("I/O error: " + e.getMessage()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 4) {
                errors.add(new WarningMigrationError("Invalid CSV line: " + line));
                return;
            }

            String id = parts[0].trim();
            String name = parts[1].trim();
            String email = parts[2].trim();
            Role role = Role.valueOf(parts[3].trim().toUpperCase());

            users.add(new User(id, name, email, role));

            percentage = users.size(); // update progress placeholder
            if (percentage >= 90) {
                errors.add(new WarningMigrationError("File warning: Near completion"));
            }
        } catch (Exception e) {
            errors.add(new WarningMigrationError("Failed to parse line: " + line));
        }
    }

    public boolean isBatchReady() {
        return batchReady;
    }

    public synchronized List<User> drainBatch() {
        List<User> drained = new ArrayList<>(users);
        users.clear();
        batchReady = false;
        notify();
        return drained;
    }

    public List<User> getRemainingUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public void shutdown() {
        closeReader(reader, "main reader");
        reader = null;
        closeReader(counter, "counter reader");
        counter = null;
    }

    private void closeReader(BufferedReader reader, String name) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                errors.add(new WarningMigrationError("Failed to close " + name + ": " + e.getMessage()));
            }
        }
    }
}
