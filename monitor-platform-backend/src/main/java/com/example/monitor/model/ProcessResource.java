package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessResource {

    private Long id;

    /**
     * 进程ID
     */
    private Long processId;

    /**
     * CPU使用率（百分比）
     */
    private Double cpuUsage;

    /**
     * 内存使用量（MB）
     */
    private Double memoryUsage;

    /**
     * 内存使用率（百分比）
     */
    private Double memoryPercent;

    /**
     * 线程数
     */
    private Integer threadCount;

    /**
     * 句柄数/文件描述符数
     */
    private Integer handleCount;

    /**
     * 磁盘读取速率（KB/s）
     */
    private Double diskReadRate;

    /**
     * 磁盘写入速率（KB/s）
     */
    private Double diskWriteRate;

    /**
     * 网络接收速率（KB/s）
     */
    private Double networkReceiveRate;

    /**
     * 网络发送速率（KB/s）
     */
    private Double networkSendRate;

    /**
     * 进程运行时间（秒）
     */
    private Long uptime;

    /**
     * 进程状态变化
     */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;
}
