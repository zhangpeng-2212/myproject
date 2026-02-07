package com.example.monitor.service;

import com.example.monitor.model.AnomalyEvent;
import com.example.monitor.model.MetricSample;
import com.example.monitor.model.ServiceInfo;
import com.example.monitor.storage.AnomalyEventFileRepository;
import com.example.monitor.storage.MetricSampleFileRepository;
import com.example.monitor.storage.ServiceInfoFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final String METRIC_NAME = "responseTime";
    private static final int SAMPLE_LIMIT = 50;
    private static final int MIN_SAMPLE_COUNT = 5;

    private final ServiceInfoFileRepository serviceInfoRepository;
    private final MetricSampleFileRepository metricSampleRepository;
    private final AnomalyEventFileRepository anomalyEventRepository;

    /**
     * 定时检测所有服务的异常情况
     */
    @Scheduled(fixedDelay = 60_000)
    public void scheduledDetect() {
        detectForAllServices();
    }

    /**
     * 对所有服务执行一次异常检测
     */
    public List<AnomalyEvent> detectForAllServices() {
        List<AnomalyEvent> created = new ArrayList<>();
        List<ServiceInfo> services = serviceInfoRepository.findAll();
        for (ServiceInfo service : services) {
            created.addAll(detectForService(service.getId()));
        }
        return created;
    }

    /**
     * 对指定服务执行一次异常检测
     */
    public List<AnomalyEvent> detectForService(Long serviceId) {
        List<MetricSample> samples = metricSampleRepository
                .findRecentByServiceAndMetric(serviceId, METRIC_NAME, SAMPLE_LIMIT);
        if (samples.size() < MIN_SAMPLE_COUNT) {
            return new ArrayList<>();
        }

        // 只用最新一个点与历史窗口进行比较
        MetricSample latest = samples.get(samples.size() - 1);
        List<MetricSample> history = samples.subList(0, samples.size() - 1);

        DoubleSummaryStatistics stats = history.stream()
                .mapToDouble(MetricSample::getValue)
                .summaryStatistics();

        double mean = stats.getAverage();
        double variance = history.stream()
                .mapToDouble(s -> {
                    double diff = s.getValue() - mean;
                    return diff * diff;
                })
                .sum() / history.size();
        double std = Math.sqrt(variance);

        double latestValue = latest.getValue();

        // 基本规则：最新值比均值高很多时认为异常
        boolean isAnomaly;
        double score;
        if (std > 0) {
            score = (latestValue - mean) / std;
            isAnomaly = score >= 3.0;
        } else {
            // 波动很小时，用相对倍率来判断
            double ratio = mean == 0 ? 0 : latestValue / mean;
            score = ratio;
            isAnomaly = ratio >= 2.0;
        }

        if (!isAnomaly) {
            return new ArrayList<>();
        }

        String severity;
        if (score >= 4.0) {
            severity = "high";
        } else if (score >= 3.0) {
            severity = "medium";
        } else {
            severity = "low";
        }

        AnomalyEvent event = new AnomalyEvent();
        event.setServiceId(serviceId);
        event.setMetricName(METRIC_NAME);
        event.setStartTime(latest.getTimestamp());
        event.setEndTime(latest.getTimestamp());
        event.setSeverity(severity);
        event.setScore(score);
        event.setReason(buildReasonText(latestValue, mean, std, score));
        event.setCreatedAt(new Date());

        anomalyEventRepository.save(event);
        return new ArrayList<>();
    }

    private String buildReasonText(double latestValue, double mean, double std, double score) {
        if (std > 0) {
            return String.format("最新值 %.2f 高于历史均值 %.2f，波动 %.2f 个标准差（score=%.2f），判定为异常。", latestValue, mean, std, score);
        } else {
            return String.format("最新值 %.2f 明显高于历史均值 %.2f（score=%.2f），判定为异常。", latestValue, mean, score);
        }
    }
}

