package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ThreadStack {

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
     * 堆栈深度
     */
    private Integer depth;

    /**
     * 完全限定类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 代码行号
     */
    private Integer lineNumber;

    /**
     * 是否是原生方法
     */
    private Boolean nativeMethod;

    /**
     * 堆栈信息
     */
    private String stackTrace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timestamp;
}
