package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ServerResource {

    private Long id;

    private Long serverId;

    /**
     * CPU使用率（百分比）
     */
    private Double cpuUsage;

    /**
     * 内存使用率（百分比）
     */
    private Double memoryUsage;

    /**
     * 内存使用量（GB）
     */
    private Double memoryUsed;

    /**
     * 磁盘使用率（百分比）
     */
    private Double diskUsage;

    /**
     * 磁盘使用量（GB）
     */
    private Double diskUsed;

    /**
     * 网络入流量（MB/s）
     */
    private Double networkIn;

    /**
     * 网络出流量（MB/s）
     */
    private Double networkOut;

    /**
     * 负载平均值
     */
    private Double loadAverage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;
}
