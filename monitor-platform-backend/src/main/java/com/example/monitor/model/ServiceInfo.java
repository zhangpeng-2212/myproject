package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
public class ServiceInfo {

    private Long id;

    private String name;

    private String env;

    private String description;

    /**
     * 预留：实际环境中可存放 metrics 端点或 Prometheus 查询信息
     */
    private String metricEndpoint;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;



}




