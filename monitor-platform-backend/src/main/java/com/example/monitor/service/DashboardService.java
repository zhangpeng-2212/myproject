package com.example.monitor.service;

import com.example.monitor.model.*;
import com.example.monitor.storage.AnomalyEventFileRepository;
import com.example.monitor.storage.MetricSampleFileRepository;
import com.example.monitor.storage.ServerInfoFileRepository;
import com.example.monitor.storage.ServiceInfoFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ServiceInfoFileRepository serviceInfoRepository;
    private final ServerInfoFileRepository serverInfoRepository;
    private final AnomalyEventFileRepository anomalyEventRepository;
    private final MetricSampleFileRepository metricSampleRepository;

    public DashboardSummary getSummary() {
        DashboardSummary summary = new DashboardSummary();

        // 服务统计
        List<ServiceInfo> services = serviceInfoRepository.findAll();
        summary.setTotalServices(services.size());
        summary.setOnlineServices((int) services.stream()
            .filter(s -> s.getEnv() != null && !s.getEnv().isEmpty())
            .count()); // 简化处理，假设有env的就是在线

        // 服务器统计
        List<ServerInfo> servers = serverInfoRepository.findAll();
        summary.setTotalServers(servers.size());
        summary.setOnlineServers((int) servers.stream()
            .filter(s -> "online".equals(s.getStatus()))
            .count());
        summary.setTotalCpuCores(servers.stream()
            .filter(s -> s.getCpuCores() != null)
            .mapToInt(ServerInfo::getCpuCores)
            .sum());
        summary.setTotalMemory(servers.stream()
            .filter(s -> s.getTotalMemory() != null)
            .mapToDouble(ServerInfo::getTotalMemory)
            .sum());
        summary.setTotalDisk(servers.stream()
            .filter(s -> s.getTotalDisk() != null)
            .mapToDouble(ServerInfo::getTotalDisk)
            .sum());

        // 异常事件统计
        Date today = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        Date yesterday = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());

        List<AnomalyEvent> allAnomalies = anomalyEventRepository.findAll();
        List<AnomalyEvent> recentAnomalies = allAnomalies.stream()
            .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().after(yesterday))
            .collect(Collectors.toList());

        summary.setRecentAnomalies(recentAnomalies.size());

        List<AnomalyEvent> todayAnomalies = allAnomalies.stream()
            .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().after(today))
            .collect(Collectors.toList());
        summary.setTodayAnomalies(todayAnomalies.size());

        summary.setHighSeverityAnomalies((int) recentAnomalies.stream()
            .filter(a -> "high".equalsIgnoreCase(a.getSeverity()))
            .count());
        summary.setMediumSeverityAnomalies((int) recentAnomalies.stream()
            .filter(a -> "medium".equalsIgnoreCase(a.getSeverity()))
            .count());
        summary.setLowSeverityAnomalies((int) recentAnomalies.stream()
            .filter(a -> "low".equalsIgnoreCase(a.getSeverity()))
            .count());

        // 按环境统计服务数
        Map<String, Integer> servicesByEnv = services.stream()
            .filter(s -> s.getEnv() != null && !s.getEnv().isEmpty())
            .collect(Collectors.groupingBy(
                ServiceInfo::getEnv,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        summary.setServicesByEnv(servicesByEnv);

        // 按类型统计服务器数
        Map<String, Integer> serversByType = servers.stream()
            .filter(s -> s.getType() != null && !s.getType().isEmpty())
            .collect(Collectors.groupingBy(
                ServerInfo::getType,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        summary.setServersByType(serversByType);

        // 最近5个异常事件
        List<AnomalyEvent> recentEvents = anomalyEventRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(5)
            .collect(Collectors.toList());
        summary.setRecentAnomalyEvents(recentEvents);

        // 服务指标趋势
        List<MetricTrend> metricTrends = new ArrayList<>();
        for (ServiceInfo service : services) {
            List<MetricSample> samples = metricSampleRepository.findRecentByServiceId(service.getId(), 50);
            if (!samples.isEmpty()) {
                MetricTrend trend = new MetricTrend();
                trend.setServiceName(service.getName());
                DoubleSummaryStatistics stats = samples.stream()
                    .filter(s -> "responseTime".equals(s.getMetricName()))
                    .mapToDouble(MetricSample::getValue)
                    .summaryStatistics();
                if (stats.getCount() > 0) {
                    trend.setAvgResponseTime(stats.getAverage());
                    trend.setMaxResponseTime(stats.getMax());
                    trend.setMinResponseTime(stats.getMin());
                    trend.setLastUpdated(samples.get(0).getTimestamp());
                    metricTrends.add(trend);
                }
            }
        }
        summary.setMetricTrends(metricTrends);

        return summary;
    }
}
