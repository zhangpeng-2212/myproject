package com.example.monitor.controller;

import com.example.monitor.model.ServerInfo;
import com.example.monitor.model.ServerResource;
import com.example.monitor.service.ServerInfoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@CrossOrigin
public class ServerController {

    private final ServerInfoService serverInfoService;

    @GetMapping
    public List<ServerInfo> listServers(@RequestParam(name = "status", required = false) String status,
                                       @RequestParam(name = "type", required = false) String type,
                                       @RequestParam(name = "env", required = false) String env) {
        if (status != null) {
            return serverInfoService.getServersByStatus(status);
        } else if (type != null) {
            return serverInfoService.getServersByType(type);
        } else if (env != null) {
            return serverInfoService.getServersByEnv(env);
        }
        return serverInfoService.getAllServers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerInfo> getServer(@PathVariable Long id) {
        ServerInfo server = serverInfoService.getServerById(id);
        if (server == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(server);
    }

    @PostMapping
    public ServerInfo createServer(@RequestBody ServerInfo serverInfo) {
        return serverInfoService.createServer(serverInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerInfo> updateServer(@PathVariable Long id, @RequestBody ServerInfo serverInfo) {
        ServerInfo updated = serverInfoService.updateServer(id, serverInfo);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverInfoService.deleteServer(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/resources")
    public List<ServerResource> getServerResources(@PathVariable Long id,
                                                   @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return serverInfoService.getRecentResources(id, limit);
    }

    @GetMapping("/{id}/resources/latest")
    public ResponseEntity<ServerResource> getLatestResource(@PathVariable Long id) {
        ServerResource resource = serverInfoService.getLatestResource(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/{id}/resources/collect")
    public ResponseEntity<Void> collectResources(@PathVariable Long id,
                                                  @RequestBody(required = false) CollectRequest request) {
        int count = (request != null && request.getCount() > 0) ? request.getCount() : 30;
        serverInfoService.generateMockResourceData(id, count);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CollectRequest {
        private int count = 30;
    }
}
