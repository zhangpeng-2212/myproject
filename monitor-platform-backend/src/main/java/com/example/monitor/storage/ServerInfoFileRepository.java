package com.example.monitor.storage;

import com.example.monitor.model.ServerInfo;
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
public class ServerInfoFileRepository {

    @Value("${monitor.storage-dir:data}")
    private String storageDir;

    private File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ServerInfo> storage = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dataFile = new File(dir, "servers.json");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!dataFile.exists()) {
            storage.clear();
            return;
        }
        try {
            List<ServerInfo> list = objectMapper.readValue(
                dataFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ServerInfo.class)
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
            log.info("Loaded {} server infos from {}", storage.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load server infos from file", e);
        }
    }

    private synchronized void saveToFile() {
        try {
            objectMapper.writeValue(dataFile, new ArrayList<>(storage.values()));
        } catch (IOException e) {
            log.error("Failed to save server infos to file", e);
        }
    }

    public ServerInfo save(ServerInfo serverInfo) {
        if (serverInfo.getId() == null) {
            serverInfo.setId(idGenerator.getAndIncrement());
            serverInfo.setCreatedAt(new Date());
        }
        serverInfo.setUpdatedAt(new Date());
        storage.put(serverInfo.getId(), serverInfo);
        saveToFile();
        return serverInfo;
    }

    public ServerInfo findById(Long id) {
        return storage.get(id);
    }

    public List<ServerInfo> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<ServerInfo> findByStatus(String status) {
        return storage.values().stream()
            .filter(s -> status == null || status.equals(s.getStatus()))
            .collect(Collectors.toList());
    }

    public List<ServerInfo> findByType(String type) {
        return storage.values().stream()
            .filter(s -> type == null || type.equals(s.getType()))
            .collect(Collectors.toList());
    }

    public List<ServerInfo> findByEnv(String env) {
        return storage.values().stream()
            .filter(s -> env == null || env.equals(s.getEnv()))
            .collect(Collectors.toList());
    }

    public void deleteById(Long id) {
        storage.remove(id);
        saveToFile();
    }

    public void clear() {
        storage.clear();
        idGenerator.set(1);
        saveToFile();
    }
}
