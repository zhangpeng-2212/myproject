package com.example.monitor.service;

import com.example.monitor.model.ServerInfo;
import com.example.monitor.model.ServerResource;
import com.example.monitor.storage.ServerInfoFileRepository;
import com.example.monitor.storage.ServerResourceFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ServerInfoService {

    private final ServerInfoFileRepository serverInfoRepository;
    private final ServerResourceFileRepository serverResourceRepository;
    private final Random random = new Random();

    public List<ServerInfo> getAllServers() {
        return serverInfoRepository.findAll();
    }

    public List<ServerInfo> getServersByStatus(String status) {
        return serverInfoRepository.findByStatus(status);
    }

    public List<ServerInfo> getServersByType(String type) {
        return serverInfoRepository.findByType(type);
    }

    public List<ServerInfo> getServersByEnv(String env) {
        return serverInfoRepository.findByEnv(env);
    }

    public ServerInfo getServerById(Long id) {
        return serverInfoRepository.findById(id);
    }

    public ServerInfo createServer(ServerInfo serverInfo) {
        if (serverInfo.getStatus() == null) {
            serverInfo.setStatus("online");
        }
        return serverInfoRepository.save(serverInfo);
    }

    public ServerInfo updateServer(Long id, ServerInfo serverInfo) {
        ServerInfo existing = serverInfoRepository.findById(id);
        if (existing == null) {
            return null;
        }
        if (serverInfo.getName() != null) existing.setName(serverInfo.getName());
        if (serverInfo.getIp() != null) existing.setIp(serverInfo.getIp());
        if (serverInfo.getType() != null) existing.setType(serverInfo.getType());
        if (serverInfo.getEnv() != null) existing.setEnv(serverInfo.getEnv());
        if (serverInfo.getDescription() != null) existing.setDescription(serverInfo.getDescription());
        if (serverInfo.getCpuCores() != null) existing.setCpuCores(serverInfo.getCpuCores());
        if (serverInfo.getTotalMemory() != null) existing.setTotalMemory(serverInfo.getTotalMemory());
        if (serverInfo.getTotalDisk() != null) existing.setTotalDisk(serverInfo.getTotalDisk());
        if (serverInfo.getStatus() != null) existing.setStatus(serverInfo.getStatus());
        return serverInfoRepository.save(existing);
    }

    public void deleteServer(Long id) {
        serverInfoRepository.deleteById(id);
    }

    /**
     * 生成模拟服务器资源数据
     * @param serverId 服务器ID
     * @param count 生成数据的数量，如果count=1则只生成当前时间的最新数据
     */
    public void generateMockResourceData(Long serverId, int count) {
        ServerInfo server = serverInfoRepository.findById(serverId);
        if (server == null) {
            return;
        }

        Date baseTime = new Date();

        // 如果只生成1条数据，时间戳设为当前时间
        // 如果生成多条数据，时间范围从当前时间倒推count分钟
        long baseTimestamp = (count == 1)
            ? baseTime.getTime()
            : baseTime.getTime() - (count * 60000L);

        for (int i = 0; i < count; i++) {
            ServerResource resource = new ServerResource();
            resource.setServerId(serverId);

            // 模拟CPU使用率 5-80%
            resource.setCpuUsage(5.0 + random.nextDouble() * 75.0);

            // 模拟内存使用率 30-90%
            double memUsage = 30.0 + random.nextDouble() * 60.0;
            resource.setMemoryUsage(memUsage);
            if (server.getTotalMemory() != null) {
                resource.setMemoryUsed(server.getTotalMemory() * memUsage / 100.0);
            }

            // 模拟磁盘使用率 20-95%
            double diskUsage = 20.0 + random.nextDouble() * 75.0;
            resource.setDiskUsage(diskUsage);
            if (server.getTotalDisk() != null) {
                resource.setDiskUsed(server.getTotalDisk() * diskUsage / 100.0);
            }

            // 模拟网络流量
            resource.setNetworkIn(random.nextDouble() * 50.0);
            resource.setNetworkOut(random.nextDouble() * 30.0);

            // 模拟负载平均值
            resource.setLoadAverage(random.nextDouble() * 3.0);

            resource.setTimestamp(new Date(baseTimestamp + i * 60000L));
            serverResourceRepository.save(resource);
        }
    }

    public List<ServerResource> getRecentResources(Long serverId, int limit) {
        return serverResourceRepository.findRecentByServerId(serverId, limit);
    }

    public ServerResource getLatestResource(Long serverId) {
        List<ServerResource> resources = serverResourceRepository.findRecentByServerId(serverId, 1);
        return resources.isEmpty() ? null : resources.get(0);
    }
}
