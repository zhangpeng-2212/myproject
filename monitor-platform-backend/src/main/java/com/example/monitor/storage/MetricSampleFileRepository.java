package com.example.monitor.storage;

import com.example.monitor.model.MetricSample;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class MetricSampleFileRepository {

    private final Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MetricSampleFileRepository(@Value("${monitor.storage-dir:data}") String storageDir) throws IOException {
        Path dir = Paths.get(storageDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        this.filePath = dir.resolve("metrics.json");
        if (!Files.exists(this.filePath)) {
            Files.write(this.filePath, "[]".getBytes(StandardCharsets.UTF_8));
        }
    }

    public MetricSample save(MetricSample sample) {
        lock.writeLock().lock();
        try {
            List<MetricSample> all = readAllInternal();
            if (sample.getId() == null) {
                long nextId = all.stream()
                        .map(MetricSample::getId)
                        .filter(id -> id != null)
                        .max(Comparator.naturalOrder())
                        .orElse(0L) + 1;
                sample.setId(nextId);
            }
            all.add(sample);
            writeAllInternal(all);
            return sample;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save metric sample", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<MetricSample> findRecentByServiceAndMetric(Long serviceId, String metricName, int limit) {
        lock.readLock().lock();
        try {
            List<MetricSample> all = readAllInternal();
            return all.stream()
                    .filter(s -> s.getServiceId().equals(serviceId)
                            && metricName.equals(s.getMetricName()))
                    .sorted(Comparator.comparing(MetricSample::getTimestamp).reversed())
                    .limit(limit)
                    .sorted(Comparator.comparing(MetricSample::getTimestamp))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metric samples", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<MetricSample> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(readAllInternal());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metric samples", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<MetricSample> findRecentByServiceId(Long serviceId, int limit) {
        lock.readLock().lock();
        try {
            List<MetricSample> all = readAllInternal();
            return all.stream()
                    .filter(s -> s.getServiceId().equals(serviceId))
                    .sorted(Comparator.comparing(MetricSample::getTimestamp).reversed())
                    .limit(limit)
                    .sorted(Comparator.comparing(MetricSample::getTimestamp))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metric samples", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<MetricSample> readAllInternal() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        String json = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (json.trim().isEmpty())  {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<MetricSample>>() {
        });
    }

    private void writeAllInternal(List<MetricSample> samples) throws IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(samples);
        Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
    }
}

