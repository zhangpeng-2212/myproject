package com.example.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 线程热点方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotspotMethod {

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 出现次数
     */
    private Integer occurrenceCount;

    /**
     * 问题类型
     */
    private String issueType;

    /**
     * 优化建议
     */
    private String suggestion;

    /**
     * 严重级别 (1-5, 5最严重)
     */
    private Integer severity;
}
