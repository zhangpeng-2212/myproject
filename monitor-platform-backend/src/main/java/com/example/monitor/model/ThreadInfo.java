package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ThreadInfo {

    private Long id;

    /**
     * 进程ID
     */
    private Long processId;

    /**
     * 线程ID
     */
    private Long threadId;

    /**
     * 线程名称
     */
    private String threadName;

    /**
     * 线程状态：RUNNABLE, WAITING, TIMED_WAITING, BLOCKED, NEW, TERMINATED
     */
    private String state;

    /**
     * 线程优先级
     */
    private Integer priority;

    /**
     * 是否守护线程
     */
    private Boolean daemon;

    /**
     * 是否存活
     */
    private Boolean alive;

    /**
     * 是否被中断
     */
    private Boolean interrupted;

    /**
     * CPU时间（毫秒）
     */
    private Long cpuTime;

    /**
     * 用户时间（毫秒）
     */
    private Long userTime;

    /**
     * 等待时间（毫秒）
     */
    private Long waitTime;

    /**
     * 阻塞时间（毫秒）
     */
    private Long blockedTime;

    /**
     * 当前执行的类名
     */
    private String currentClass;

    /**
     * 当前执行的方法名
     */
    private String currentMethod;

    /**
     * 当前执行的代码行号
     */
    private Integer currentLine;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;
}
