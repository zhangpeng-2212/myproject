package com.example.monitor.controller;

import com.example.monitor.model.MetricSample;
import com.example.monitor.service.MetricService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin
public class MetricController {

    private final MetricService metricService;

    @GetMapping("/{serviceId}")
    public List<MetricSample> getRecentMetrics(@PathVariable Long serviceId,
                                               @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return metricService.getRecentMetrics(serviceId, limit);
    }

    @PostMapping("/collect")
    public ResponseEntity<Void> collectMetrics(@RequestBody CollectRequest request) {
        metricService.collectMockMetricsForService(request.getServiceId(), request.getCount());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CollectRequest {
        private Long serviceId;
        private int count = 30;
    }
}

