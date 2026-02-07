package com.example.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * AI Agent数据模型
 * 作为监控平台的统一数据实例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgent {
    
    /**
     * Agent ID
     */
    private Long id;
    
    /**
     * Agent名称
     */
    private String name;
    
    /**
     * Agent类型
     * server - 服务器监控Agent
     * process - 进程监控Agent
     * thread - 线程监控Agent
     * hotspot - 热点分析Agent
     */
    private String type;
    
    /**
     * Agent状态
     * idle - 空闲
     * active - 活跃
     * analyzing - 分析中
     * error - 错误
     */
    private String status;
    
    /**
     * 监控目标ID（服务器ID/进程ID等）
     */
    private Long targetId;
    
    /**
     * 目标名称（如服务器名称、进程名称）
     */
    private String targetName;
    
    /**
     * Agent能力列表（JSON数组格式）
     */
    private String capabilities;
    
    /**
     * 最后活动时间
     */
    private Date lastActiveTime;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * Agent配置（JSON格式）
     */
    private String config;
    
    /**
     * 健康评分（0-100）
     */
    private Integer healthScore;
    
    /**
     * 性能指标摘要（JSON格式）
     */
    private String metricsSummary;
}
