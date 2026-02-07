package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
public class AnomalyEvent {

    private Long id;

    private Long serviceId;

    private String metricName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * low / medium / high
     */
    private String severity;

    /**
     * 异常评分，例如 (value - mean) / std
     */
    private double score;

    /**
     * 简要原因说明
     */
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}

