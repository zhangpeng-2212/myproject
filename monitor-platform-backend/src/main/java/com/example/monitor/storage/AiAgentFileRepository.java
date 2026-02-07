package com.example.monitor.storage;

import com.example.monitor.model.AiAgent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Agent文件仓储
 */
@Slf4j
@Repository
public class AiAgentFileRepository {
    
    private static final String DATA_FILE = "data/ai-agents.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<AiAgent> cache = new ArrayList<>();
    
    public AiAgentFileRepository() {
        loadData();
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try {
                List<AiAgent> agents = objectMapper.readValue(file, 
                    new TypeReference<List<AiAgent>>() {});
                cache.addAll(agents);
                log.info("已加载 {} 个AI Agent", cache.size());
            } catch (IOException e) {
                log.error("加载AI Agent数据失败", e);
            }
        }
    }
    
    /**
     * 保存数据
     */
    private void saveData() {
        try {
            File file = new File(DATA_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file, cache);
        } catch (IOException e) {
            log.error("保存AI Agent数据失败", e);
        }
    }
    
    /**
     * 查找所有Agent
     */
    public List<AiAgent> findAll() {
        return new ArrayList<>(cache);
    }
    
    /**
     * 根据类型查找Agent
     */
    public List<AiAgent> findByType(String type) {
        return cache.stream()
            .filter(agent -> type.equals(agent.getType()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据状态查找Agent
     */
    public List<AiAgent> findByStatus(String status) {
        return cache.stream()
            .filter(agent -> status.equals(agent.getStatus()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据目标ID查找Agent
     */
    public List<AiAgent> findByTargetId(Long targetId) {
        return cache.stream()
            .filter(agent -> targetId.equals(agent.getTargetId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查找Agent
     */
    public AiAgent findById(Long id) {
        return cache.stream()
            .filter(agent -> id.equals(agent.getId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 保存Agent
     */
    public AiAgent save(AiAgent agent) {
        if (agent.getId() == null) {
            agent.setId(generateId());
            agent.setCreatedAt(new java.util.Date());
        }
        agent.setUpdatedAt(new java.util.Date());
        
        // 如果已存在则更新
        for (int i = 0; i < cache.size(); i++) {
            if (agent.getId().equals(cache.get(i).getId())) {
                cache.set(i, agent);
                saveData();
                return agent;
            }
        }
        
        // 否则添加
        cache.add(agent);
        saveData();
        return agent;
    }
    
    /**
     * 删除Agent
     */
    public void delete(Long id) {
        cache.removeIf(agent -> id.equals(agent.getId()));
        saveData();
    }
    
    /**
     * 删除目标的所有Agent
     */
    public void deleteByTargetId(Long targetId) {
        cache.removeIf(agent -> targetId.equals(agent.getTargetId()));
        saveData();
    }
    
    /**
     * 生成ID
     */
    private Long generateId() {
        return cache.stream()
            .mapToLong(AiAgent::getId)
            .max()
            .orElse(0L) + 1;
    }
    
    /**
     * 查找最新的Agent（按更新时间）
     */
    public List<AiAgent> findLatest(int limit) {
        return cache.stream()
            .sorted(Comparator.comparing(AiAgent::getUpdatedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
