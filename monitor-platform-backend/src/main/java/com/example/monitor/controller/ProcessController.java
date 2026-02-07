package com.example.monitor.controller;

import com.example.monitor.model.ProcessInfo;
import com.example.monitor.model.ProcessResource;
import com.example.monitor.model.ThreadInfo;
import com.example.monitor.model.ThreadStack;
import com.example.monitor.service.ProcessInfoService;
import com.example.monitor.service.ThreadInfoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
@CrossOrigin
public class ProcessController {

    private final ProcessInfoService processInfoService;
    private final ThreadInfoService threadInfoService;

    @GetMapping
    public List<ProcessInfo> listProcesses(@RequestParam(name = "serverId", required = false) Long serverId,
                                          @RequestParam(name = "status", required = false) String status,
                                          @RequestParam(name = "type", required = false) String type) {
        if (serverId != null) {
            return processInfoService.getProcessesByServerId(serverId);
        } else if (status != null) {
            return processInfoService.getProcessesByStatus(status);
        } else if (type != null) {
            return processInfoService.getProcessesByType(type);
        }
        return processInfoService.getAllProcesses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessInfo> getProcess(@PathVariable Long id) {
        ProcessInfo process = processInfoService.getProcessById(id);
        if (process == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(process);
    }

    @PostMapping
    public ProcessInfo createProcess(@RequestBody ProcessInfo processInfo) {
        return processInfoService.createProcess(processInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcessInfo> updateProcess(@PathVariable Long id, @RequestBody ProcessInfo processInfo) {
        ProcessInfo updated = processInfoService.updateProcess(id, processInfo);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProcess(@PathVariable Long id) {
        processInfoService.deleteProcess(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startProcess(@PathVariable Long id) {
        boolean success = processInfoService.startProcess(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "进程启动成功" : "进程启动失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopProcess(@PathVariable Long id) {
        boolean success = processInfoService.stopProcess(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "进程停止成功" : "进程停止失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartProcess(@PathVariable Long id) {
        boolean success = processInfoService.restartProcess(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "进程重启成功" : "进程重启失败");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/resources")
    public List<ProcessResource> getProcessResources(@PathVariable Long id,
                                                     @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return processInfoService.getRecentResources(id, limit);
    }

    @GetMapping("/{id}/resources/latest")
    public ResponseEntity<ProcessResource> getLatestResource(@PathVariable Long id) {
        ProcessResource resource = processInfoService.getLatestResource(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/{id}/resources/collect")
    public ResponseEntity<Void> collectResources(@PathVariable Long id,
                                                @RequestBody(required = false) CollectRequest request) {
        int count = (request != null && request.getCount() > 0) ? request.getCount() : 30;
        processInfoService.generateMockResourceData(id, count);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getStatsSummary() {
        List<ProcessInfo> allProcesses = processInfoService.getAllProcesses();

        long total = allProcesses.size();
        long running = allProcesses.stream().filter(p -> "running".equals(p.getStatus())).count();
        long stopped = allProcesses.stream().filter(p -> "stopped".equals(p.getStatus())).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("running", running);
        summary.put("stopped", stopped);
        summary.put("error", allProcesses.stream().filter(p -> "error".equals(p.getStatus())).count());

        return ResponseEntity.ok(summary);
    }

    @Data
    public static class CollectRequest {
        private int count = 30;
    }

    @GetMapping("/{id}/threads")
    public List<ThreadInfo> getProcessThreads(@PathVariable Long id) {
        return threadInfoService.getLatestThreads(id);
    }

    @GetMapping("/{id}/threads/{threadId}/stack")
    public List<ThreadStack> getThreadStack(@PathVariable Long id,
                                          @PathVariable Long threadId) {
        return threadInfoService.getThreadStacks(id, threadId);
    }

    @PostMapping("/{id}/threads/collect")
    public ResponseEntity<Void> collectThreadData(@PathVariable Long id,
                                              @RequestBody(required = false) ThreadCollectRequest request) {
        int threadCount = (request != null && request.getThreadCount() > 0)
            ? request.getThreadCount() : 20;
        threadInfoService.generateMockThreadData(id, threadCount);
        return ResponseEntity.ok().build();
    }

    /**
     * 生成热点测试数据（用于测试AI分析功能）
     * POST /api/processes/{id}/threads/collect-hotspot
     */
    @PostMapping("/{id}/threads/collect-hotspot")
    public ResponseEntity<Map<String, Object>> collectHotspotTestData(@PathVariable Long id) {
        threadInfoService.generateHotspotMockData(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "热点测试数据生成成功");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/threads/stats")
    public ResponseEntity<Map<String, Object>> getThreadStats(@PathVariable Long id) {
        Map<String, Object> stats = threadInfoService.analyzeThreadStats(id);
        return ResponseEntity.ok(stats);
    }

    @Data
    public static class ThreadCollectRequest {
        private int threadCount = 20;
    }
}
