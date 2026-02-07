package com.example.monitor.controller;

import com.example.monitor.model.AnomalyEvent;
import com.example.monitor.service.AnomalyDetectionService;
import com.example.monitor.storage.AnomalyEventFileRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@CrossOrigin
public class AnomalyController {

    private final AnomalyEventFileRepository anomalyEventRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    public List<AnomalyEvent> listAnomalies(@RequestParam(name = "serviceId", required = false) Long serviceId,
                                            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return anomalyEventRepository.findRecentByService(serviceId, limit);
    }

    /**
     * 触发一次异常检测。
     * 如果 body 中包含 serviceId，则仅检测该服务；否则检测所有服务。
     */
    @PostMapping("/detect")
    public List<AnomalyEvent> detect(@RequestBody(required = false) DetectRequest request) {
        if (request != null && request.getServiceId() != null) {
            return anomalyDetectionService.detectForService(request.getServiceId());
        }
        return anomalyDetectionService.detectForAllServices();
    }

    @Data
    public static class DetectRequest {
        private Long serviceId;
    }
}

