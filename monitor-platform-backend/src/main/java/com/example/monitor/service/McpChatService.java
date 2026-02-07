package com.example.monitor.service;

import com.example.monitor.model.*;
import com.example.monitor.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpChatService {

    private final ServiceInfoFileRepository serviceInfoRepository;
    private final ServerInfoFileRepository serverInfoRepository;
    private final AnomalyEventFileRepository anomalyEventRepository;
    private final MetricSampleFileRepository metricSampleRepository;
    private final ServerResourceFileRepository serverResourceRepository;

    public String processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "请提供您的问题。";
        }

        String lowerQuery = query.toLowerCase();

        // 服务相关查询
        if (lowerQuery.contains("服务") || lowerQuery.contains("service")) {
            return handleServiceQuery(query);
        }

        // 服务器相关查询
        if (lowerQuery.contains("服务器") || lowerQuery.contains("server")) {
            return handleServerQuery(query);
        }

        // 异常相关查询
        if (lowerQuery.contains("异常") || lowerQuery.contains("anomaly") || lowerQuery.contains("告警")) {
            return handleAnomalyQuery(query);
        }

        // 指标相关查询
        if (lowerQuery.contains("指标") || lowerQuery.contains("metric") || lowerQuery.contains("响应时间")) {
            return handleMetricQuery(query);
        }

        // 资源相关查询
        if (lowerQuery.contains("资源") || lowerQuery.contains("cpu") || lowerQuery.contains("内存") || lowerQuery.contains("disk")) {
            return handleResourceQuery(query);
        }

        // 健康状态查询
        if (lowerQuery.contains("健康") || lowerQuery.contains("状态") || lowerQuery.contains("health")) {
            return handleHealthQuery(query);
        }

        // 统计查询
        if (lowerQuery.contains("统计") || lowerQuery.contains("数量") || lowerQuery.contains("总")) {
            return handleStatisticsQuery(query);
        }

        return "抱歉，我目前可以帮助您查询以下信息：\n" +
               "1. 服务信息（服务数量、列表等）\n" +
               "2. 服务器信息（服务器数量、资源使用等）\n" +
               "3. 异常事件（最近异常、告警统计等）\n" +
               "4. 监控指标（响应时间、指标数据等）\n" +
               "5. 资源使用（CPU、内存、磁盘等）\n" +
               "6. 系统健康状态\n" +
               "7. 统计信息\n\n" +
               "请具体说明您想了解什么信息。";
    }

    private String handleServiceQuery(String query) {
        List<ServiceInfo> services = serviceInfoRepository.findAll();

        if (query.contains("多少") || query.contains("数量") || query.contains("总数")) {
            return "当前共有 " + services.size() + " 个服务。";
        }

        if (query.contains("列表")) {
            StringBuilder sb = new StringBuilder("服务列表：\n");
            services.forEach(s -> {
                sb.append(String.format("- %s (环境: %s, 描述: %s)\n",
                    s.getName(), s.getEnv() != null ? s.getEnv() : "未设置",
                    s.getDescription() != null ? s.getDescription() : "无"));
            });
            return sb.toString();
        }

        if (query.contains("prod") || query.contains("生产")) {
            long count = services.stream().filter(s -> "prod".equalsIgnoreCase(s.getEnv())).count();
            return "生产环境共有 " + count + " 个服务。";
        }

        if (query.contains("test") || query.contains("测试")) {
            long count = services.stream().filter(s -> "test".equalsIgnoreCase(s.getEnv())).count();
            return "测试环境共有 " + count + " 个服务。";
        }

        return "当前共有 " + services.size() + " 个服务。您想了解具体哪个服务的信息吗？";
    }

    private String handleServerQuery(String query) {
        List<ServerInfo> servers = serverInfoRepository.findAll();

        if (query.contains("多少") || query.contains("数量") || query.contains("总数")) {
            return "当前共有 " + servers.size() + " 台服务器。";
        }

        if (query.contains("在线") || query.contains("online")) {
            long onlineCount = servers.stream().filter(s -> "online".equals(s.getStatus())).count();
            return "当前有 " + onlineCount + " 台服务器在线，" + (servers.size() - onlineCount) + " 台离线。";
        }

        if (query.contains("列表")) {
            StringBuilder sb = new StringBuilder("服务器列表：\n");
            servers.forEach(s -> {
                sb.append(String.format("- %s (IP: %s, 类型: %s, 状态: %s)\n",
                    s.getName(), s.getIp() != null ? s.getIp() : "未设置",
                    s.getType() != null ? s.getType() : "未设置",
                    s.getStatus()));
            });
            return sb.toString();
        }

        if (query.contains("类型") || query.contains("type")) {
            Map<String, Long> byType = servers.stream()
                .filter(s -> s.getType() != null)
                .collect(Collectors.groupingBy(ServerInfo::getType, Collectors.counting()));
            StringBuilder sb = new StringBuilder("服务器按类型统计：\n");
            byType.forEach((type, count) -> sb.append(String.format("- %s: %d 台\n", type, count)));
            return sb.toString();
        }

        return "当前共有 " + servers.size() + " 台服务器。您想了解哪方面的服务器信息？";
    }

    private String handleAnomalyQuery(String query) {
        List<AnomalyEvent> anomalies = anomalyEventRepository.findAll();

        if (anomalies.isEmpty()) {
            return "当前没有异常事件记录。";
        }

        if (query.contains("多少") || query.contains("数量")) {
            return "系统中共有 " + anomalies.size() + " 条异常事件记录。";
        }

        if (query.contains("最近") || query.contains("latest")) {
            List<AnomalyEvent> recent = anomalies.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder("最近5条异常事件：\n");
            recent.forEach(a -> {
                sb.append(String.format("- [%s] %s 服务: %s, 评分: %.2f, 原因: %s\n",
                    a.getSeverity(), a.getCreatedAt(), a.getServiceId(), a.getScore(), a.getReason()));
            });
            return sb.toString();
        }

        if (query.contains("高") || query.contains("严重")) {
            long highCount = anomalies.stream()
                .filter(a -> "high".equalsIgnoreCase(a.getSeverity()))
                .count();
            List<AnomalyEvent> highAnomalies = anomalies.stream()
                .filter(a -> "high".equalsIgnoreCase(a.getSeverity()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder("共 " + highCount + " 条高严重级别异常。最近5条：\n");
            highAnomalies.forEach(a -> {
                sb.append(String.format("- %s 服务: %s, 评分: %.2f, 原因: %s\n",
                    a.getCreatedAt(), a.getServiceId(), a.getScore(), a.getReason()));
            });
            return sb.toString();
        }

        return "系统中共有 " + anomalies.size() + " 条异常事件记录。";
    }

    private String handleMetricQuery(String query) {
        List<ServiceInfo> services = serviceInfoRepository.findAll();

        if (query.contains("响应时间") || query.contains("response")) {
            Map<String, Double> avgResponseTimes = new HashMap<>();
            for (ServiceInfo service : services) {
                List<MetricSample> samples = metricSampleRepository.findRecentByServiceId(service.getId(), 50);
                OptionalDouble avg = samples.stream()
                    .filter(s -> "responseTime".equals(s.getMetricName()))
                    .mapToDouble(MetricSample::getValue)
                    .average();
                avg.ifPresent(value -> avgResponseTimes.put(service.getName(), value));
            }

            if (avgResponseTimes.isEmpty()) {
                return "暂无响应时间数据。";
            }

            StringBuilder sb = new StringBuilder("各服务平均响应时间：\n");
            avgResponseTimes.forEach((name, time) -> {
                sb.append(String.format("- %s: %.2f ms\n", name, time));
            });
            return sb.toString();
        }

        return "暂无相关指标数据。您可以查询响应时间等信息。";
    }

    private String handleResourceQuery(String query) {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        List<ServerResource> latestResources = serverResourceRepository.findRecent(servers.size());

        if (latestResources.isEmpty()) {
            return "暂无资源使用数据。";
        }

        if (query.contains("cpu")) {
            StringBuilder sb = new StringBuilder("服务器CPU使用率：\n");
            latestResources.forEach(r -> {
                ServerInfo server = servers.stream()
                    .filter(s -> s.getId().equals(r.getServerId()))
                    .findFirst().orElse(null);
                if (server != null) {
                    sb.append(String.format("- %s: %.1f%%\n", server.getName(), r.getCpuUsage()));
                }
            });
            return sb.toString();
        }

        if (query.contains("内存") || query.contains("memory")) {
            StringBuilder sb = new StringBuilder("服务器内存使用情况：\n");
            latestResources.forEach(r -> {
                ServerInfo server = servers.stream()
                    .filter(s -> s.getId().equals(r.getServerId()))
                    .findFirst().orElse(null);
                if (server != null) {
                    sb.append(String.format("- %s: %.1f%% (%.2f GB / %.2f GB)\n",
                        server.getName(), r.getMemoryUsage(),
                        r.getMemoryUsed(), server.getTotalMemory()));
                }
            });
            return sb.toString();
        }

        if (query.contains("磁盘") || query.contains("disk")) {
            StringBuilder sb = new StringBuilder("服务器磁盘使用情况：\n");
            latestResources.forEach(r -> {
                ServerInfo server = servers.stream()
                    .filter(s -> s.getId().equals(r.getServerId()))
                    .findFirst().orElse(null);
                if (server != null) {
                    sb.append(String.format("- %s: %.1f%% (%.2f GB / %.2f GB)\n",
                        server.getName(), r.getDiskUsage(),
                        r.getDiskUsed(), server.getTotalDisk()));
                }
            });
            return sb.toString();
        }

        return "您可以查询CPU、内存、磁盘等资源使用情况。";
    }

    private String handleHealthQuery(String query) {
        List<ServerInfo> servers = serverInfoRepository.findAll();
        long onlineServers = servers.stream().filter(s -> "online".equals(s.getStatus())).count();
        long offlineServers = servers.size() - onlineServers;

        List<AnomalyEvent> recentAnomalies = anomalyEventRepository.findAll().stream()
            .filter(a -> {
                Date today = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
                return a.getCreatedAt() != null && a.getCreatedAt().after(today);
            })
            .collect(Collectors.toList());

        long highSeverity = recentAnomalies.stream()
            .filter(a -> "high".equalsIgnoreCase(a.getSeverity()))
            .count();

        StringBuilder sb = new StringBuilder("系统健康状态：\n");
        sb.append(String.format("- 服务器状态: %d/%d 在线\n", onlineServers, servers.size()));
        sb.append(String.format("- 最近24小时异常: %d 条\n", recentAnomalies.size()));
        sb.append(String.format("- 高严重级别异常: %d 条\n", highSeverity));

        if (highSeverity == 0 && offlineServers == 0) {
            sb.append("\n✓ 系统运行正常，无严重异常。");
        } else if (highSeverity > 0) {
            sb.append("\n⚠ 系统存在高严重级别异常，需要关注。");
        } else if (offlineServers > 0) {
            sb.append("\n⚠ 有服务器离线，需要检查。");
        }

        return sb.toString();
    }

    private String handleStatisticsQuery(String query) {
        List<ServiceInfo> services = serviceInfoRepository.findAll();
        List<ServerInfo> servers = serverInfoRepository.findAll();
        List<AnomalyEvent> anomalies = anomalyEventRepository.findAll();

        StringBuilder sb = new StringBuilder("系统统计概览：\n");
        sb.append(String.format("服务数量: %d\n", services.size()));
        sb.append(String.format("服务器数量: %d\n", servers.size()));
        sb.append(String.format("异常事件总数: %d\n", anomalies.size()));

        Map<String, Long> byEnv = services.stream()
            .filter(s -> s.getEnv() != null)
            .collect(Collectors.groupingBy(ServiceInfo::getEnv, Collectors.counting()));
        if (!byEnv.isEmpty()) {
            sb.append("\n按环境分布：\n");
            byEnv.forEach((env, count) -> sb.append(String.format("- %s: %d\n", env, count)));
        }

        return sb.toString();
    }
}
