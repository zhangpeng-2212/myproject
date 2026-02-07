package com.example.monitor.storage;

import com.example.monitor.model.ProcessInfo;
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
public class ProcessInfoFileRepository {

    @Value("${monitor.storage-dir:data}")
    private String storageDir;

    private File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ProcessInfo> storage = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dataFile = new File(dir, "processes.json");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!dataFile.exists()) {
            storage.clear();
            return;
        }
        try {
            List<ProcessInfo> list = objectMapper.readValue(
                dataFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ProcessInfo.class)
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
            log.info("Loaded {} process infos from {}", storage.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load process infos from file", e);
        }
    }

    private synchronized void saveToFile() {
        try {
            objectMapper.writeValue(dataFile, new ArrayList<>(storage.values()));
        } catch (IOException e) {
            log.error("Failed to save process infos to file", e);
        }
    }

    public ProcessInfo save(ProcessInfo processInfo) {
        if (processInfo.getId() == null) {
            processInfo.setId(idGenerator.getAndIncrement());
            processInfo.setCreatedAt(new Date());
        }
        processInfo.setUpdatedAt(new Date());
        storage.put(processInfo.getId(), processInfo);
        saveToFile();
        return processInfo;
    }

    public ProcessInfo findById(Long id) {
        return storage.get(id);
    }

    public List<ProcessInfo> findAll() {
        // 按自定义顺序排序：Java应用服务、MySQL、Nginx排在前面，其他按ID排序
        return storage.values().stream()
            .sorted((p1, p2) -> {
                int order1 = getProcessOrder(p1.getName());
                int order2 = getProcessOrder(p2.getName());
                if (order1 != order2) {
                    return Integer.compare(order1, order2);
                }
                // 相同优先级按ID排序
                return Long.compare(p1.getId(), p2.getId());
            })
            .collect(Collectors.toList());
    }

    private int getProcessOrder(String name) {
        if (name == null) return 100;
        // 核心进程优先级
        if (name.contains("Java应用服务")) return 1;
        if (name.contains("MySQL")) return 2;
        if (name.contains("Nginx")) return 3;
        // 其他进程按ID排序
        return 100;
    }

    public List<ProcessInfo> findByServerId(Long serverId) {
        return storage.values().stream()
            .filter(p -> serverId == null || serverId.equals(p.getServerId()))
            .sorted((p1, p2) -> {
                int order1 = getProcessOrder(p1.getName());
                int order2 = getProcessOrder(p2.getName());
                if (order1 != order2) {
                    return Integer.compare(order1, order2);
                }
                return Long.compare(p1.getId(), p2.getId());
            })
            .collect(Collectors.toList());
    }

    public List<ProcessInfo> findByStatus(String status) {
        return storage.values().stream()
            .filter(p -> status == null || status.equals(p.getStatus()))
            .sorted((p1, p2) -> {
                int order1 = getProcessOrder(p1.getName());
                int order2 = getProcessOrder(p2.getName());
                if (order1 != order2) {
                    return Integer.compare(order1, order2);
                }
                return Long.compare(p1.getId(), p2.getId());
            })
            .collect(Collectors.toList());
    }

    public List<ProcessInfo> findByType(String type) {
        return storage.values().stream()
            .filter(p -> type == null || type.equals(p.getType()))
            .sorted((p1, p2) -> {
                int order1 = getProcessOrder(p1.getName());
                int order2 = getProcessOrder(p2.getName());
                if (order1 != order2) {
                    return Integer.compare(order1, order2);
                }
                return Long.compare(p1.getId(), p2.getId());
            })
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
