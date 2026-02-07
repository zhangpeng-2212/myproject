package com.example.monitor.storage;

import com.example.monitor.model.AnomalyEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class AnomalyEventFileRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public AnomalyEventFileRepository(@Value("${monitor.storage-dir:data}") String storageDir) throws IOException {
        Path dir = Paths.get(storageDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        this.filePath = dir.resolve("anomalies.json");
        if (!Files.exists(this.filePath)) {
            Files.write(this.filePath, "[]".getBytes(StandardCharsets.UTF_8));
        }
    }

    public AnomalyEvent save(AnomalyEvent event) {
        lock.writeLock().lock();
        try {
            List<AnomalyEvent> all = readAllInternal();
            if (event.getId() == null) {
                long nextId = all.stream()
                        .map(AnomalyEvent::getId)
                        .filter(id -> id != null)
                        .max(Comparator.naturalOrder())
                        .orElse(0L) + 1;
                event.setId(nextId);
            }

            Optional<AnomalyEvent> existingOpt = all.stream()
                    .filter(e -> e.getId().equals(event.getId()))
                    .findFirst();
            existingOpt.ifPresent(all::remove);

            all.add(event);
            writeAllInternal(all);
            return event;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save anomaly event", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<AnomalyEvent> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(readAllInternal());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read anomaly events", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<AnomalyEvent> findRecentByService(Long serviceId, int limit) {
        lock.readLock().lock();
        try {
            return readAllInternal().stream()
                    .filter(e -> serviceId == null || serviceId.equals(e.getServiceId()))
                    .sorted(Comparator.comparing(AnomalyEvent::getCreatedAt).reversed())
                    .limit(limit)
                    .sorted(Comparator.comparing(AnomalyEvent::getCreatedAt))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read anomaly events", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<AnomalyEvent> readAllInternal() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<AnomalyEvent>>() {
        });
    }

    private void writeAllInternal(List<AnomalyEvent> events) throws IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(events);
        Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
    }
}

