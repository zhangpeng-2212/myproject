package com.example.monitor.service;

import com.example.monitor.model.ProcessInfo;
import com.example.monitor.model.ProcessResource;
import com.example.monitor.storage.ProcessInfoFileRepository;
import com.example.monitor.storage.ProcessResourceFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInfoService {

    private final ProcessInfoFileRepository processInfoRepository;
    private final ProcessResourceFileRepository processResourceRepository;
    private final Random random = new Random();

    public List<ProcessInfo> getAllProcesses() {
        return processInfoRepository.findAll();
    }

    public List<ProcessInfo> getProcessesByServerId(Long serverId) {
        return processInfoRepository.findByServerId(serverId);
    }

    public List<ProcessInfo> getProcessesByStatus(String status) {
        return processInfoRepository.findByStatus(status);
    }

    public List<ProcessInfo> getProcessesByType(String type) {
        return processInfoRepository.findByType(type);
    }

    public ProcessInfo getProcessById(Long id) {
        return processInfoRepository.findById(id);
    }

    public ProcessInfo createProcess(ProcessInfo processInfo) {
        if (processInfo.getStatus() == null) {
            processInfo.setStatus("stopped");
        }
        return processInfoRepository.save(processInfo);
    }

    public ProcessInfo updateProcess(Long id, ProcessInfo processInfo) {
        ProcessInfo existing = processInfoRepository.findById(id);
        if (existing == null) {
            return null;
        }
        if (processInfo.getName() != null) existing.setName(processInfo.getName());
        if (processInfo.getPid() != null) existing.setPid(processInfo.getPid());
        if (processInfo.getCommand() != null) existing.setCommand(processInfo.getCommand());
        if (processInfo.getUser() != null) existing.setUser(processInfo.getUser());
        if (processInfo.getType() != null) existing.setType(processInfo.getType());
        if (processInfo.getPorts() != null) existing.setPorts(processInfo.getPorts());
        if (processInfo.getStatus() != null) existing.setStatus(processInfo.getStatus());
        if (processInfo.getStartCommand() != null) existing.setStartCommand(processInfo.getStartCommand());
        if (processInfo.getStopCommand() != null) existing.setStopCommand(processInfo.getStopCommand());
        if (processInfo.getDescription() != null) existing.setDescription(processInfo.getDescription());
        if (processInfo.getAutoStart() != null) existing.setAutoStart(processInfo.getAutoStart());
        return processInfoRepository.save(existing);
    }

    public void deleteProcess(Long id) {
        processInfoRepository.deleteById(id);
    }

    /**
     * 启动进程（优雅启动）
     */
    public boolean startProcess(Long id) {
        ProcessInfo process = processInfoRepository.findById(id);
        if (process == null) {
            return false;
        }

        try {
            // 这里可以实际执行启动命令
            if (process.getStartCommand() != null && !process.getStartCommand().isEmpty()) {
                log.info("Starting process {} with command: {}", process.getName(), process.getStartCommand());
                // 实际环境中可以执行: Runtime.getRuntime().exec(process.getStartCommand());
            }

            // 更新进程状态
            process.setStatus("running");
            process.setPid(String.valueOf(1000 + random.nextInt(9000))); // 模拟生成PID
            processInfoRepository.save(process);
            return true;
        } catch (Exception e) {
            log.error("Failed to start process {}", id, e);
            return false;
        }
    }

    /**
     * 停止进程（优雅停止）
     */
    public boolean stopProcess(Long id) {
        ProcessInfo process = processInfoRepository.findById(id);
        if (process == null) {
            return false;
        }

        try {
            // 这里可以实际执行停止命令
            if (process.getStopCommand() != null && !process.getStopCommand().isEmpty()) {
                log.info("Stopping process {} with command: {}", process.getName(), process.getStopCommand());
                // 实际环境中可以执行: Runtime.getRuntime().exec(process.getStopCommand());
            } else if (process.getPid() != null) {
                // 默认使用kill命令
                log.info("Stopping process {} with PID: {}", process.getName(), process.getPid());
                // 实际环境中可以执行: Runtime.getRuntime().exec("kill " + process.getPid());
            }

            // 更新进程状态
            process.setStatus("stopped");
            process.setPid(null);
            processInfoRepository.save(process);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop process {}", id, e);
            return false;
        }
    }

    /**
     * 重启进程
     */
    public boolean restartProcess(Long id) {
        if (!stopProcess(id)) {
            return false;
        }
        // 等待一小段时间
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return startProcess(id);
    }

    /**
     * 生成模拟进程资源数据
     */
    public void generateMockResourceData(Long processId, int count) {
        ProcessInfo process = processInfoRepository.findById(processId);
        if (process == null || !"running".equals(process.getStatus())) {
            return;
        }

        Date baseTime = new Date();
        long baseTimestamp = baseTime.getTime() - (count * 60000L); // 从count分钟前开始

        for (int i = 0; i < count; i++) {
            ProcessResource resource = new ProcessResource();
            resource.setProcessId(processId);

            // 模拟CPU使用率 0-30%
            resource.setCpuUsage(random.nextDouble() * 30.0);

            // 模拟内存使用量 50-500MB
            resource.setMemoryUsage(50.0 + random.nextDouble() * 450.0);

            // 模拟内存使用率 1-15%
            resource.setMemoryPercent(1.0 + random.nextDouble() * 14.0);

            // 模拟线程数 5-50
            resource.setThreadCount(5 + random.nextInt(46));

            // 模拟句柄数 50-500
            resource.setHandleCount(50 + random.nextInt(451));

            // 模拟磁盘读写速率
            resource.setDiskReadRate(random.nextDouble() * 100.0);
            resource.setDiskWriteRate(random.nextDouble() * 50.0);

            // 模拟网络收发速率
            resource.setNetworkReceiveRate(random.nextDouble() * 200.0);
            resource.setNetworkSendRate(random.nextDouble() * 100.0);

            // 模拟运行时间（小时）
            resource.setUptime((long) (1 + random.nextDouble() * 1000));

            resource.setStatus("running");
            resource.setTimestamp(new Date(baseTimestamp + i * 60000L));
            processResourceRepository.save(resource);
        }
    }

    public List<ProcessResource> getRecentResources(Long processId, int limit) {
        return processResourceRepository.findRecentByProcessId(processId, limit);
    }

    public ProcessResource getLatestResource(Long processId) {
        List<ProcessResource> resources = processResourceRepository.findRecentByProcessId(processId, 1);
        return resources.isEmpty() ? null : resources.get(0);
    }
}
