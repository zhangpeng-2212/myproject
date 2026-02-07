package com.example.monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 线程热点分析结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreadHotspotAnalysis {

    /**
     * 进程ID
     */
    private Long processId;

    /**
     * 分析时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date analysisTime;

    /**
     * 线程总数
     */
    private Integer totalThreads;

    /**
     * Top N热点方法
     */
    private List<HotspotMethod> topHotspots;

    /**
     * 分析摘要
     */
    private String summary;

    /**
     * 总体健康评分 (0-100)
     */
    private Integer healthScore;
}
