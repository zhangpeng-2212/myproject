package com.example.monitor.controller;

import com.example.monitor.model.ServiceInfo;
import com.example.monitor.service.ServiceInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin
public class ServiceController {

    private final ServiceInfoService serviceInfoService;

    @GetMapping
    public List<ServiceInfo> listServices() {
        return serviceInfoService.listAll();
    }

    @PostMapping
    public ResponseEntity<ServiceInfo> createService(@RequestBody ServiceInfo request) {
        ServiceInfo created = serviceInfoService.create(request);
        return ResponseEntity.ok(created);
    }
}

