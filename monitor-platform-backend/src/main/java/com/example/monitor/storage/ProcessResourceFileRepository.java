package com.example.monitor.storage;

import com.example.monitor.model.ProcessResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ProcessResourceFileRepository {

    @Value("${monitor.storage-dir:data}")
    private String storageDir;

    private File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ProcessResource> storage = new ConcurrentHashMap<>();

    private static final int MAX_HISTORY = 10000;

    @PostConstruct
    public void init() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dataFile = new File(dir, "process-resources.json");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!dataFile.exists()) {
            storage.clear();
            return;
        }

        // 检查文件是否为空
        if (dataFile.length() == 0) {
            storage.clear();
            log.info("Process resource file is empty, starting with empty storage");
            return;
        }

        try {
            List<ProcessResource> list = objectMapper.readValue(
                dataFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ProcessResource.class)
            );
            storage.clear();
            if (list != null) {
                list.forEach(item -> {
                    storage.put(item.getId(), item);
                    if (item.getId() >= idGenerator.get()) {
                        idGenerator.set(item.getId() + 1);
                    }
                });
            }
            log.info("Loaded {} process resources from {}", storage.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load process resources from file", e);
            // 即使加载失败，也清空存储以避免不一致状态
            storage.clear();
        }
    }

    private synchronized void saveToFile() {
        try {
            List<ProcessResource> list = new ArrayList<>(storage.values());
            if (list.size() > MAX_HISTORY) {
                list = list.stream()
                    .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                    .skip(list.size() - MAX_HISTORY)
                    .collect(Collectors.toList());
            }
            objectMapper.writeValue(dataFile, list);
        } catch (IOException e) {
            log.error("Failed to save process resources to file", e);
        }
    }

    public ProcessResource save(ProcessResource resource) {
        if (resource.getId() == null) {
            resource.setId(idGenerator.getAndIncrement());
        }
        if (resource.getTimestamp() == null) {
            resource.setTimestamp(new Date());
        }
        storage.put(resource.getId(), resource);
        saveToFile();
        return resource;
    }

    public List<ProcessResource> findByProcessId(Long processId) {
        return storage.values().stream()
            .filter(r -> processId == null || processId.equals(r.getProcessId()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
    }

    public List<ProcessResource> findRecentByProcessId(Long processId, int limit) {
        return findByProcessId(processId).stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ProcessResource> findRecent(int limit) {
        return storage.values().stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public void clear() {
        storage.clear();
        idGenerator.set(1);
        saveToFile();
    }
}
