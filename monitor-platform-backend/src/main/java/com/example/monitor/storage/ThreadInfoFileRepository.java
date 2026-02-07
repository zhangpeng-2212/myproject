package com.example.monitor.storage;

import com.example.monitor.model.ThreadInfo;
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
public class ThreadInfoFileRepository {

    @Value("${monitor.storage-dir:data}")
    private String storageDir;

    private File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ThreadInfo> storage = new ConcurrentHashMap<>();

    private static final int MAX_HISTORY = 10000;

    @PostConstruct
    public void init() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dataFile = new File(dir, "threads.json");
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
            log.info("Thread info file is empty, starting with empty storage");
            return;
        }

        try {
            List<ThreadInfo> list = objectMapper.readValue(
                dataFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ThreadInfo.class)
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
            log.info("Loaded {} thread infos from {}", storage.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load thread infos from file", e);
            // 即使加载失败，也清空存储以避免不一致状态
            storage.clear();
        }
    }

    private synchronized void saveToFile() {
        try {
            List<ThreadInfo> list = new ArrayList<>(storage.values());
            if (list.size() > MAX_HISTORY) {
                list = list.stream()
                    .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                    .skip(list.size() - MAX_HISTORY)
                    .collect(Collectors.toList());
            }
            objectMapper.writeValue(dataFile, list);
        } catch (IOException e) {
            log.error("Failed to save thread infos to file", e);
        }
    }

    public ThreadInfo save(ThreadInfo threadInfo) {
        if (threadInfo.getId() == null) {
            threadInfo.setId(idGenerator.getAndIncrement());
        }
        if (threadInfo.getTimestamp() == null) {
            threadInfo.setTimestamp(new Date());
        }
        storage.put(threadInfo.getId(), threadInfo);
        saveToFile();
        return threadInfo;
    }

    public List<ThreadInfo> findByProcessId(Long processId) {
        return storage.values().stream()
            .filter(t -> processId == null || processId.equals(t.getProcessId()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
    }

    public List<ThreadInfo> findLatestByProcessId(Long processId) {
        // 获取每个线程的最新记录
        return storage.values().stream()
            .filter(t -> processId == null || processId.equals(t.getProcessId()))
            .collect(Collectors.toMap(
                ThreadInfo::getThreadId,
                t -> t,
                (a, b) -> a.getTimestamp().after(b.getTimestamp()) ? a : b
            ))
            .values()
            .stream()
            .sorted((a, b) -> a.getThreadId().compareTo(b.getThreadId()))
            .collect(Collectors.toList());
    }

    public void clear() {
        storage.clear();
        idGenerator.set(1);
        saveToFile();
    }

    /**
     * 清除指定进程的线程数据
     */
    public void clearByProcessId(Long processId) {
        storage.values().removeIf(t -> processId.equals(t.getProcessId()));
        saveToFile();
    }
}
