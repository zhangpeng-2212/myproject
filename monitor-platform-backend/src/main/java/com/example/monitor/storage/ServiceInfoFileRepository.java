package com.example.monitor.storage;

import com.example.monitor.model.ServiceInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ServiceInfoFileRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ServiceInfoFileRepository(@Value("${monitor.storage-dir:data}") String storageDir) throws IOException {
        Path dir = Paths.get(storageDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        this.filePath = dir.resolve("services.json");
        if (!Files.exists(this.filePath)) {
            Files.write(this.filePath, "[]".getBytes(StandardCharsets.UTF_8));
        }
    }

    public List<ServiceInfo> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(readAllInternal());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read services from file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public ServiceInfo save(ServiceInfo serviceInfo) {
        lock.writeLock().lock();
        try {
            List<ServiceInfo> all = readAllInternal();

            if (serviceInfo.getId() == null) {
                long nextId = all.stream()
                        .map(ServiceInfo::getId)
                        .filter(id -> id != null)
                        .max(Comparator.naturalOrder())
                        .orElse(0L) + 1;
                serviceInfo.setId(nextId);
            }
            if (serviceInfo.getCreatedAt() == null) {
                serviceInfo.setCreatedAt(new Date());
            }

            Optional<ServiceInfo> existingOpt = all.stream()
                    .filter(s -> s.getId().equals(serviceInfo.getId()))
                    .findFirst();
            if (existingOpt.isPresent()) {
                all.remove(existingOpt.get());
            }
            all.add(serviceInfo);

            writeAllInternal(all);
            return serviceInfo;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save service to file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<ServiceInfo> findById(Long id) {
        lock.readLock().lock();
        try {
            return readAllInternal().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read services from file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<ServiceInfo> readAllInternal() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<ServiceInfo>>() {
        });
    }

    private void writeAllInternal(List<ServiceInfo> services) throws IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(services);
        Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
    }
}

