package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessInfo {

    private Long id;

    /**
     * 所属服务器ID
     */
    private Long serverId;

    /**
     * 进程名称
     */
    private String name;

    /**
     * 进程ID
     */
    private String pid;

    /**
     * 进程命令
     */
    private String command;

    /**
     * 进程用户
     */
    private String user;

    /**
     * 进程类型：app / database / cache / system / other
     */
    private String type;

    /**
     * 进程端口（多个端口用逗号分隔）
     */
    private String ports;

    /**
     * 进程状态：running / stopped / error
     */
    private String status;

    /**
     * 启动命令
     */
    private String startCommand;

    /**
     * 停止命令
     */
    private String stopCommand;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否自动启动
     */
    private Boolean autoStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
