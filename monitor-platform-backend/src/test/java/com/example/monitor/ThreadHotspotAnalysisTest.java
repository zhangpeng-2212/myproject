package com.example.monitor;

import com.example.monitor.model.ThreadHotspotAnalysis;
import com.example.monitor.service.ThreadHotspotAnalysisService;
import com.example.monitor.service.ThreadInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 线程热点AI分析功能测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ThreadHotspotAnalysisTest {

    @Autowired
    private ThreadInfoService threadInfoService;

    @Autowired
    private ThreadHotspotAnalysisService hotspotAnalysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试热点分析功能
     */
    @Test
    public void testHotspotAnalysis() throws Exception {
        // 1. 进程ID（假设进程ID=1存在）
        Long processId = 1L;

        // 2. 生成热点测试数据
        System.out.println("\n========================================");
        System.out.println("步骤1: 生成热点测试数据");
        System.out.println("========================================");
        threadInfoService.generateHotspotMockData(processId);
        System.out.println("✓ 热点测试数据生成成功\n");

        // 3. 执行热点分析
        System.out.println("========================================");
        System.out.println("步骤2: 执行热点分析");
        System.out.println("========================================");
        ThreadHotspotAnalysis analysis = hotspotAnalysisService.analyze(processId);

        // 4. 打印分析结果
        System.out.println("\n分析结果:");
        System.out.println("----------------------------------------");
        System.out.println("进程ID: " + analysis.getProcessId());
        System.out.println("分析时间: " + analysis.getAnalysisTime());
        System.out.println("线程总数: " + analysis.getTotalThreads());
        System.out.println("健康评分: " + analysis.getHealthScore() + "/100");
        System.out.println("摘要: " + analysis.getSummary());
        System.out.println("\nTop 10 热点方法:");
        System.out.println("----------------------------------------");

        int rank = 1;
        for (com.example.monitor.model.HotspotMethod hotspot : analysis.getTopHotspots()) {
            System.out.printf("%2d. %s.%s\n", rank++, hotspot.getClassName(), hotspot.getMethodName());
            System.out.printf("   出现次数: %d\n", hotspot.getOccurrenceCount());
            System.out.printf("   问题类型: %s\n", hotspot.getIssueType());
            System.out.printf("   严重级别: %s (1-5)\n", "⭐".repeat(hotspot.getSeverity()));
            System.out.printf("   优化建议: %s\n", hotspot.getSuggestion());
            System.out.println();
        }

        // 5. 输出JSON格式结果
        System.out.println("========================================");
        System.out.println("JSON格式结果:");
        System.out.println("========================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis));
        System.out.println("\n========================================");
        System.out.println("测试完成！");
        System.out.println("========================================\n");
    }

    /**
     * 测试分析空数据
     */
    @Test
    public void testEmptyAnalysis() throws Exception {
        Long processId = 999L; // 不存在的进程

        System.out.println("\n========================================");
        System.out.println("测试: 分析不存在的进程");
        System.out.println("========================================");

        ThreadHotspotAnalysis analysis = hotspotAnalysisService.analyze(processId);

        System.out.println("分析结果:");
        System.out.println("- 线程总数: " + analysis.getTotalThreads());
        System.out.println("- 摘要: " + analysis.getSummary());
        System.out.println("- 健康评分: " + analysis.getHealthScore());
        System.out.println("✓ 空数据分析正常\n");
    }

    /**
     * 打印测试使用说明
     */
    @Test
    public void printUsageGuide() {
        System.out.println("\n========================================");
        System.out.println("线程热点AI分析 - 使用说明");
        System.out.println("========================================");
        System.out.println("\n1. API接口:");
        System.out.println("   POST /api/processes/{processId}/threads/collect-hotspot");
        System.out.println("   - 生成热点测试数据");
        System.out.println("\n   POST /api/processes/{processId}/threads/analyze");
        System.out.println("   - 执行热点分析");
        System.out.println("\n2. 测试步骤:");
        System.out.println("   1) 启动应用: mvn spring-boot:run");
        System.out.println("   2) 生成数据: curl -X POST http://localhost:8080/api/processes/1/threads/collect-hotspot");
        System.out.println("   3) 执行分析: curl -X POST http://localhost:8080/api/processes/1/threads/analyze");
        System.out.println("\n3. 热点类型:");
        System.out.println("   - 并发安全: HashMap, ArrayList等");
        System.out.println("   - 锁等待/竞争: wait, lock等");
        System.out.println("   - 线程池: ThreadPoolExecutor等");
        System.out.println("   - 性能优化: sleep, IO操作等");
        System.out.println("   - 数据库: SQL查询等");
        System.out.println("\n4. 严重级别:");
        System.out.println("   5 - 并发安全、死锁风险");
        System.out.println("   4 - 锁等待、锁竞争");
        System.out.println("   3 - 线程池、数据库");
        System.out.println("   2 - IO操作、网络");
        System.out.println("   1 - 常规调用");
        System.out.println("\n5. 健康评分:");
        System.out.println("   80-100 - 良好");
        System.out.println("   60-79  - 一般");
        System.out.println("   0-59   - 需关注");
        System.out.println("========================================\n");
    }
}
