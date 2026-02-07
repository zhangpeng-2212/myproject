package com.example.monitor.service;

import com.example.monitor.model.ServiceInfo;
import com.example.monitor.storage.ServiceInfoFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceInfoService {

    private final ServiceInfoFileRepository repository;

    public List<ServiceInfo> listAll() {
        return repository.findAll();
    }

    public ServiceInfo create(ServiceInfo request) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(request.getName());
        serviceInfo.setEnv(request.getEnv());
        serviceInfo.setDescription(request.getDescription());
        serviceInfo.setMetricEndpoint(request.getMetricEndpoint());
        return repository.save(serviceInfo);
    }

    public Optional<ServiceInfo> findById(Long id) {
        return repository.findById(id);
    }
}

