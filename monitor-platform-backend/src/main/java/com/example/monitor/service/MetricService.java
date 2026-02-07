package com.example.monitor.service;

import com.example.monitor.model.MetricSample;
import com.example.monitor.storage.MetricSampleFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MetricService {

    private static final String DEFAULT_METRIC_NAME = "responseTime";

    private final MetricSampleFileRepository repository;
    private final Random random = new Random();

    /**
     * 模拟采集：为指定服务生成若干条响应时间数据
     */
    public void collectMockMetricsForService(Long serviceId, int count) {
        Instant now = Instant.now();
        for (int i = count - 1; i >= 0; i--) {
            MetricSample sample = new MetricSample();
            sample.setServiceId(serviceId);
            sample.setMetricName(DEFAULT_METRIC_NAME);

            // 生成时间点，间隔 10 秒
            sample.setTimestamp(Date.from(now.minusSeconds(i * 10L)));

            // 基本值在 100~300ms 之间，带一点随机波动
            double base = 100 + random.nextDouble() * 200;
            // 偶尔制造一点高延迟
            if (random.nextDouble() < 0.1) {
                base += 300 + random.nextDouble() * 400;
            }
            sample.setValue(base);

            repository.save(sample);
        }
    }

    public List<MetricSample> getRecentMetrics(Long serviceId, int limit) {
        return repository.findRecentByServiceAndMetric(serviceId, DEFAULT_METRIC_NAME, limit);
    }
}

