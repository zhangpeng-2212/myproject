package com.example.monitor.service;

import com.example.monitor.model.HotspotMethod;
import com.example.monitor.model.ThreadHotspotAnalysis;
import com.example.monitor.model.ThreadInfo;
import com.example.monitor.model.ThreadStack;
import com.example.monitor.storage.ThreadInfoFileRepository;
import com.example.monitor.storage.ThreadStackFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 线程热点分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadHotspotAnalysisService {

    private final ThreadInfoFileRepository threadInfoRepository;
    private final ThreadStackFileRepository threadStackRepository;

    private static final int TOP_N = 10;

    /**
     * 分析指定进程的线程热点
     */
    public ThreadHotspotAnalysis analyze(Long processId) {
        log.info("开始分析进程 {} 的线程热点", processId);

        // 1. 获取所有线程
        List<ThreadInfo> threads = threadInfoRepository.findLatestByProcessId(processId);
        if (threads.isEmpty()) {
            log.warn("进程 {} 没有线程数据", processId);
            return createEmptyAnalysis(processId);
        }

        // 2. 统计所有线程堆栈中的热点方法
        Map<String, HotspotCounter> hotspotCount = analyzeHotspots(threads, processId);

        // 3. 排序取Top N
        List<HotspotMethod> topHotspots = hotspotCount.values().stream()
                .sorted((a, b) -> b.getCount() - a.getCount())
                .limit(TOP_N)
                .map(this::buildHotspotMethod)
                .collect(Collectors.toList());

        // 4. 计算健康评分
        int healthScore = calculateHealthScore(topHotspots);

        // 5. 生成分析摘要
        String summary = generateSummary(topHotspots, threads.size(), healthScore);

        // 6. 构建结果
        ThreadHotspotAnalysis result = new ThreadHotspotAnalysis();
        result.setProcessId(processId);
        result.setAnalysisTime(new Date());
        result.setTotalThreads(threads.size());
        result.setTopHotspots(topHotspots);
        result.setSummary(summary);
        result.setHealthScore(healthScore);

        log.info("线程热点分析完成，共发现 {} 个热点方法", topHotspots.size());
        return result;
    }

    /**
     * 分析热点方法
     */
    private Map<String, HotspotCounter> analyzeHotspots(List<ThreadInfo> threads, Long processId) {
        Map<String, HotspotCounter> hotspotCount = new HashMap<>();

        for (ThreadInfo thread : threads) {
            List<ThreadStack> stacks = threadStackRepository.findByThreadId(processId, thread.getThreadId());

            for (ThreadStack stack : stacks) {
                String className = stack.getClassName();
                String methodName = stack.getMethodName();
                String key = className + "." + methodName;

                HotspotCounter counter = hotspotCount.get(key);
                if (counter == null) {
                    counter = new HotspotCounter(className, methodName, 0);
                    hotspotCount.put(key, counter);
                }
                counter.increment();
            }
        }

        return hotspotCount;
    }

    /**
     * 构建热点方法对象
     */
    private HotspotMethod buildHotspotMethod(HotspotCounter counter) {
        String className = counter.getClassName();
        String methodName = counter.getMethodName();
        int count = counter.getCount();

        // 判断问题类型
        String issueType = analyzeIssueType(className, methodName, count);

        // 生成建议
        String suggestion = generateSuggestion(className, methodName, issueType, count);

        // 评估严重级别
        int severity = evaluateSeverity(issueType, count);

        return new HotspotMethod(className, methodName, count, issueType, suggestion, severity);
    }

    /**
     * 分析问题类型
     */
    private String analyzeIssueType(String className, String methodName, int count) {
        // 并发安全相关
        if (className.contains("HashMap") || className.contains("ArrayList") || className.contains("HashSet")) {
            return "并发安全";
        }
        if (className.contains("ConcurrentHashMap") && methodName.contains("compute")) {
            return "并发性能";
        }
        if (className.contains("Thread") && methodName.equals("sleep")) {
            return "性能优化";
        }
        if (className.contains("Object") && methodName.equals("wait")) {
            return "锁等待";
        }

        // 线程池相关
        if (className.contains("ThreadPoolExecutor") || className.contains("ForkJoinPool")) {
            return "线程池";
        }
        if (className.contains("LinkedBlockingQueue") || className.contains("ArrayBlockingQueue")) {
            return "队列";
        }

        // 锁相关
        if (className.contains("ReentrantLock") || className.contains("synchronized")) {
            return "锁竞争";
        }
        if (className.contains("AbstractQueuedSynchronizer")) {
            return "锁等待";
        }

        // IO相关
        if (className.contains("InputStream") || className.contains("OutputStream")) {
            return "IO操作";
        }
        if (className.contains("Socket") || className.contains("Connection")) {
            return "网络IO";
        }
        if (className.contains("File")) {
            return "文件IO";
        }

        // 数据库相关 - MySQL特定的检测
        if (className.contains("com.mysql.cj.jdbc.MysqlIO") || className.contains("com.mysql.jdbc.MysqlIO")) {
            if (methodName.equals("sendCommand")) {
                return "MySQL网络IO";
            }
            if (methodName.equals("read") || methodName.equals("write")) {
                return "MySQL读写";
            }
            return "MySQL通信";
        }
        if (className.contains("ConnectionImpl") && methodName.equals("isValid")) {
            return "连接验证";
        }
        if (className.contains("HikariPool") && methodName.equals("getConnection")) {
            return "连接池等待";
        }
        if (className.contains("HikariDataSource") && methodName.equals("getConnection")) {
            return "连接获取";
        }
        if (className.contains("JdbcTemplate") && methodName.equals("query")) {
            return "JDBC查询";
        }
        if (className.contains("MyBatis") && methodName.equals("query")) {
            return "MyBatis查询";
        }
        if (className.contains("jdbc") || className.contains("mysql") || className.contains("Statement")) {
            return "数据库";
        }
        if (className.contains("PreparedStatement") && (methodName.contains("execute") || methodName.contains("query"))) {
            return "SQL性能";
        }

        // HTTP相关
        if (className.contains("HttpServlet") || className.contains("DispatcherServlet")) {
            return "HTTP请求";
        }

        // 集合框架
        if (className.contains("Map") || className.contains("List") || className.contains("Set")) {
            return "集合操作";
        }

        // 根据调用次数判断
        if (count > 20) {
            return "频繁调用";
        }

        return "常规调用";
    }

    /**
     * 生成优化建议
     */
    private String generateSuggestion(String className, String methodName, String issueType, int count) {
        switch (issueType) {
            case "并发安全":
                return "建议使用ConcurrentHashMap或添加synchronized保护";

            case "并发性能":
                return "避免使用computeIfAbsent等复杂并发操作，考虑分片或加锁";

            case "性能优化":
                return "频繁的sleep调用影响响应时间，考虑使用事件驱动或异步";

            case "锁等待":
                return "存在大量wait操作，检查锁持有时间和死锁风险";

            case "线程池":
                if (methodName.equals("getTask")) {
                    return "线程池等待队列较长，考虑增加核心线程数或优化任务";
                }
                return "线程池配置可能不合理，建议调整参数";

            case "队列":
                return "队列操作频繁，检查队列大小和消费者性能";

            case "锁竞争":
                return "存在锁竞争，考虑使用读写锁或减小锁粒度";

            case "IO操作":
                return "IO操作频繁，建议使用NIO或异步IO";

            case "网络IO":
                return "网络IO阻塞较多，考虑连接池或超时控制";

            case "MySQL网络IO":
                return "MySQL网络通信频繁，建议检查网络延迟、压缩传输或批量操作";

            case "MySQL读写":
                return "MySQL读写操作频繁，考虑使用索引、优化查询语句或增加缓存";

            case "MySQL通信":
                return "MySQL通信开销较大，检查查询效率、减少往返次数";

            case "连接验证":
                return "连接验证频繁（isValid调用），考虑使用连接池心跳检查替代";

            case "连接池等待":
                return "连接池获取连接等待较长，建议增加最大连接数或优化查询执行时间";

            case "连接获取":
                return "频繁获取数据库连接，确保连接池配置合理（最大连接数、空闲超时）";

            case "JDBC查询":
                return "JDBC查询频繁，建议添加索引、优化SQL语句或使用批处理";

            case "MyBatis查询":
                return "MyBatis查询频繁，建议启用二级缓存、优化映射配置或使用懒加载";

            case "数据库":
                return "数据库操作频繁，检查SQL性能和连接池配置";

            case "SQL性能":
                return "SQL执行频繁，建议添加索引、优化查询语句或使用缓存";

            case "HTTP请求":
                return "HTTP请求处理较慢，考虑缓存或异步处理";

            case "集合操作":
                if (methodName.contains("put") || methodName.contains("add")) {
                    return "集合写入频繁，考虑扩容或使用并发集合";
                }
                return "集合操作频繁，检查算法复杂度";

            case "频繁调用":
                return "调用非常频繁（" + count + "次），建议优化算法或增加缓存";

            default:
                return "常规调用，无需优化";
        }
    }

    /**
     * 评估严重级别 (1-5)
     */
    private int evaluateSeverity(String issueType, int count) {
        int baseSeverity = 1;

        // 根据问题类型设置基础严重级别
        switch (issueType) {
            case "并发安全":
                baseSeverity = 5;
                break;
            case "锁等待":
            case "锁竞争":
            case "连接池等待":
                baseSeverity = 4;
                break;
            case "性能优化":
            case "线程池":
            case "数据库":
            case "MySQL网络IO":
            case "MySQL读写":
                baseSeverity = 3;
                break;
            case "IO操作":
            case "网络IO":
            case "SQL性能":
            case "MySQL通信":
            case "连接获取":
            case "连接验证":
            case "JDBC查询":
            case "MyBatis查询":
                baseSeverity = 2;
                break;
            default:
                baseSeverity = 1;
        }

        // 根据调用次数调整严重级别
        if (count >= 20) {
            baseSeverity = Math.min(5, baseSeverity + 1);
        } else if (count >= 10) {
            // 保持不变
        } else if (count <= 3) {
            baseSeverity = Math.max(1, baseSeverity - 1);
        }

        return baseSeverity;
    }

    /**
     * 计算健康评分
     */
    private int calculateHealthScore(List<HotspotMethod> topHotspots) {
        if (topHotspots.isEmpty()) {
            return 100;
        }

        // 根据严重级别扣分
        int totalPenalty = 0;
        for (HotspotMethod hotspot : topHotspots) {
            int penalty = 0;
            switch (hotspot.getSeverity()) {
                case 5:
                    penalty = 20;
                    break;
                case 4:
                    penalty = 15;
                    break;
                case 3:
                    penalty = 10;
                    break;
                case 2:
                    penalty = 5;
                    break;
                case 1:
                    penalty = 2;
                    break;
            }
            totalPenalty += penalty;
        }

        int score = Math.max(0, 100 - totalPenalty);
        return score;
    }

    /**
     * 生成分析摘要
     */
    private String generateSummary(List<HotspotMethod> topHotspots, int totalThreads, int healthScore) {
        if (topHotspots.isEmpty()) {
            return "未检测到热点方法，系统运行正常。";
        }

        int highPriorityCount = (int) topHotspots.stream()
                .filter(h -> h.getSeverity() >= 4)
                .count();

        int mediumPriorityCount = (int) topHotspots.stream()
                .filter(h -> h.getSeverity() == 3)
                .count();

        StringBuilder sb = new StringBuilder();
        sb.append("检测到 ").append(topHotspots.size()).append(" 个热点方法");

        if (highPriorityCount > 0) {
            sb.append("，其中 ").append(highPriorityCount).append(" 个高优先级问题");
        }
        if (mediumPriorityCount > 0) {
            sb.append("，").append(mediumPriorityCount).append(" 个中优先级问题");
        }

        sb.append("。健康评分：").append(healthScore).append("分");

        if (healthScore >= 80) {
            sb.append("（良好）");
        } else if (healthScore >= 60) {
            sb.append("（一般）");
        } else {
            sb.append("（需关注）");
        }

        return sb.toString();
    }

    /**
     * 创建空分析结果
     */
    private ThreadHotspotAnalysis createEmptyAnalysis(Long processId) {
        ThreadHotspotAnalysis result = new ThreadHotspotAnalysis();
        result.setProcessId(processId);
        result.setAnalysisTime(new Date());
        result.setTotalThreads(0);
        result.setTopHotspots(new ArrayList<>());
        result.setSummary("当前进程没有线程数据，无法进行热点分析。");
        result.setHealthScore(100);
        return result;
    }

    /**
     * 热点计数器（内部类）
     */
    private static class HotspotCounter {
        private String className;
        private String methodName;
        private int count;

        public HotspotCounter(String className, String methodName, int count) {
            this.className = className;
            this.methodName = methodName;
            this.count = count;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getCount() {
            return count;
        }

        public void increment() {
            this.count++;
        }
    }
}
