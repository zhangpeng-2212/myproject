package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class MetricTrend {

    private String serviceName;

    private Double avgResponseTime;

    private Double maxResponseTime;

    private Double minResponseTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdated;
}
