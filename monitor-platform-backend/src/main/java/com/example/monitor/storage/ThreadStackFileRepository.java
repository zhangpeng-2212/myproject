package com.example.monitor.storage;

import com.example.monitor.model.ThreadStack;
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
public class ThreadStackFileRepository {

    @Value("${monitor.storage-dir:data}")
    private String storageDir;

    private File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, ThreadStack> storage = new ConcurrentHashMap<>();

    private static final int MAX_HISTORY = 50000;

    @PostConstruct
    public void init() {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dataFile = new File(dir, "thread-stacks.json");
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
            log.info("Thread stack file is empty, starting with empty storage");
            return;
        }

        try {
            List<ThreadStack> list = objectMapper.readValue(
                dataFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ThreadStack.class)
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
            log.info("Loaded {} thread stacks from {}", storage.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load thread stacks from file", e);
            // 即使加载失败，也清空存储以避免不一致状态
            storage.clear();
        }
    }

    private synchronized void saveToFile() {
        try {
            List<ThreadStack> list = new ArrayList<>(storage.values());
            if (list.size() > MAX_HISTORY) {
                list = list.stream()
                    .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                    .skip(list.size() - MAX_HISTORY)
                    .collect(Collectors.toList());
            }
            objectMapper.writeValue(dataFile, list);
        } catch (IOException e) {
            log.error("Failed to save thread stacks to file", e);
        }
    }

    public ThreadStack save(ThreadStack threadStack) {
        if (threadStack.getId() == null) {
            threadStack.setId(idGenerator.getAndIncrement());
        }
        if (threadStack.getTimestamp() == null) {
            threadStack.setTimestamp(new Date());
        }
        storage.put(threadStack.getId(), threadStack);
        saveToFile();
        return threadStack;
    }

    public List<ThreadStack> findByThreadId(Long processId, Long threadId) {
        return storage.values().stream()
            .filter(t -> (processId == null || processId.equals(t.getProcessId())) &&
                       (threadId == null || threadId.equals(t.getThreadId())))
            .sorted((a, b) -> b.getDepth().compareTo(a.getDepth()))
            .collect(Collectors.toList());
    }

    public void clear() {
        storage.clear();
        idGenerator.set(1);
        saveToFile();
    }

    /**
     * 清除指定进程的线程堆栈数据
     */
    public void clearByProcessId(Long processId) {
        storage.values().removeIf(t -> processId.equals(t.getProcessId()));
        saveToFile();
    }
}
