package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
public class MetricSample {

    private Long id;

    private Long serviceId;

    /**
     * 指标名称，MVP 版本可以固定为 responseTime
     */
    private String metricName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;

    private double value;
}

