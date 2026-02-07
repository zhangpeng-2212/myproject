package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ServerInfo {

    private Long id;

    private String name;

    private String ip;

    /**
     * 服务器类型：app / db / cache / message-queue 等
     */
    private String type;

    private String env;

    private String description;

    /**
     * 总CPU核心数
     */
    private Integer cpuCores;

    /**
     * 总内存（GB）
     */
    private Double totalMemory;

    /**
     * 总磁盘空间（GB）
     */
    private Double totalDisk;

    /**
     * 状态：online / offline / maintenance
     */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
