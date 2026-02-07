package com.example.monitor.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardSummary {

    /**
     * 总服务数
     */
    private Integer totalServices;

    /**
     * 在线服务数
     */
    private Integer onlineServices;

    /**
     * 总服务器数
     */
    private Integer totalServers;

    /**
     * 在线服务器数
     */
    private Integer onlineServers;

    /**
     * 服务器总CPU核心数
     */
    private Integer totalCpuCores;

    /**
     * 服务器总内存（GB）
     */
    private Double totalMemory;

    /**
     * 服务器总磁盘（GB）
     */
    private Double totalDisk;

    /**
     * 最近24小时异常事件数
     */
    private Integer recentAnomalies;

    /**
     * 今日异常事件数
     */
    private Integer todayAnomalies;

    /**
     * 高严重级别异常数
     */
    private Integer highSeverityAnomalies;

    /**
     * 中严重级别异常数
     */
    private Integer mediumSeverityAnomalies;

    /**
     * 低严重级别异常数
     */
    private Integer lowSeverityAnomalies;

    /**
     * 按环境统计的服务数
     */
    private Map<String, Integer> servicesByEnv;

    /**
     * 按类型统计的服务器数
     */
    private Map<String, Integer> serversByType;

    /**
     * 最近5个异常事件
     */
    private List<AnomalyEvent> recentAnomalyEvents;

    /**
     * 最近5个服务指标趋势（平均响应时间）
     */
    private List<MetricTrend> metricTrends;
}
