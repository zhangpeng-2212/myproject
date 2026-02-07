package com.example.monitor.controller;

import com.example.monitor.model.ThreadHotspotAnalysis;
import com.example.monitor.service.ThreadHotspotAnalysisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 线程分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/processes/{processId}/threads")
@RequiredArgsConstructor
@CrossOrigin
public class ThreadAnalysisController {

    private final ThreadHotspotAnalysisService hotspotAnalysisService;

    /**
     * 分析线程热点
     * POST /api/processes/{processId}/threads/analyze
     */
    @PostMapping("/analyze")
    public ApiResponse<ThreadHotspotAnalysis> analyze(@PathVariable Long processId) {
        log.info("收到进程 {} 的线程热点分析请求", processId);

        try {
            ThreadHotspotAnalysis result = hotspotAnalysisService.analyze(processId);
            return new ApiResponse<>(200, "分析成功", result);
        } catch (Exception e) {
            log.error("线程热点分析失败", e);
            return new ApiResponse<>(500, "分析失败：" + e.getMessage(), null);
        }
    }

    /**
     * 统一响应格式
     */
    @Data
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }
}
