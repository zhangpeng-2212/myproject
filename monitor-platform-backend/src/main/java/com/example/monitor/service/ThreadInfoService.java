package com.example.monitor.service;

import com.example.monitor.model.ProcessInfo;
import com.example.monitor.model.ThreadInfo;
import com.example.monitor.model.ThreadStack;
import com.example.monitor.storage.ProcessInfoFileRepository;
import com.example.monitor.storage.ThreadInfoFileRepository;
import com.example.monitor.storage.ThreadStackFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThreadInfoService {

    private final ThreadInfoFileRepository threadInfoRepository;
    private final ThreadStackFileRepository threadStackRepository;
    private final ProcessInfoFileRepository processInfoRepository;
    private final Random random = new Random();

    private static final String[] COMMON_CLASSES = {
        "java.lang.Thread",
        "java.util.concurrent.ThreadPoolExecutor",
        "java.lang.Object",
        "java.util.concurrent.locks.AbstractQueuedSynchronizer",
        "com.example.app.service",
        "com.example.app.controller",
        "org.springframework.web.servlet.DispatcherServlet",
        "java.util.HashMap",
        "java.util.concurrent.LinkedBlockingQueue",
        "com.mysql.cj.jdbc.ConnectionImpl",
        "com.mysql.cj.jdbc.ClientPreparedStatement",
        "com.mysql.cj.jdbc.MysqlIO",
        "com.mysql.cj.protocol.a.NativeProtocol",
        "org.springframework.jdbc.core.JdbcTemplate",
        "com.zaxxer.hikari.HikariDataSource",
        "com.zaxxer.hikari.pool.HikariPool",
        "org.apache.ibatis.session.defaults.DefaultSqlSession",
        "org.apache.ibatis.executor.BaseExecutor"
    };

    private static final String[] COMMON_METHODS = {
        "run",
        "execute",
        "getTask",
        "await",
        "park",
        "handleRequest",
        "doService",
        "put",
        "take",
        "lock",
        "unlock",
        "executeQuery",
        "executeUpdate",
        "sendCommand",
        "read",
        "write",
        "getConnection",
        "close",
        "query",
        "update"
    };

    private static final String[] THREAD_STATES = {
        "RUNNABLE",
        "WAITING",
        "TIMED_WAITING",
        "BLOCKED"
    };

    /**
     * 获取进程的最新线程列表
     */
    public List<ThreadInfo> getLatestThreads(Long processId) {
        return threadInfoRepository.findLatestByProcessId(processId);
    }

    /**
     * 清除指定进程的线程数据
     */
    private void clearProcessThreadData(Long processId) {
        threadInfoRepository.clearByProcessId(processId);
        threadStackRepository.clearByProcessId(processId);
    }

    /**
     * 获取线程的堆栈信息
     */
    public List<ThreadStack> getThreadStacks(Long processId, Long threadId) {
        return threadStackRepository.findByThreadId(processId, threadId);
    }

    /**
     * 生成模拟线程数据
     */
    public void generateMockThreadData(Long processId, int threadCount) {
        ProcessInfo process = processInfoRepository.findById(processId);
        if (process == null || !"running".equals(process.getStatus())) {
            return;
        }

        // 只清除当前进程的旧数据
        clearProcessThreadData(processId);

        for (int i = 0; i < threadCount; i++) {
            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.setProcessId(processId);
            threadInfo.setThreadId((long) (1000 + i));

            // 随机生成线程名称
            String[] threadTypes = {
                "pool-" + i + "-thread",
                "worker-" + i,
                "async-task-" + i,
                "gc-task-" + i,
                "http-nio-" + (8080 + i) + "-exec-" + i,
                "scheduler-thread-" + i,
                "monitor-thread-" + i
            };
            threadInfo.setThreadName(threadTypes[random.nextInt(threadTypes.length)]);

            // 随机线程状态
            String state = THREAD_STATES[random.nextInt(THREAD_STATES.length)];
            threadInfo.setState(state);

            // 优先级 1-10
            threadInfo.setPriority(random.nextInt(10) + 1);

            // 随机设置布尔属性
            threadInfo.setDaemon(random.nextBoolean());
            threadInfo.setAlive(true);
            threadInfo.setInterrupted(random.nextBoolean());

            // CPU时间（秒）
            threadInfo.setCpuTime((long) (random.nextDouble() * 100000));
            threadInfo.setUserTime((long) (random.nextDouble() * 100000));

            // 等待和阻塞时间
            if ("WAITING".equals(state) || "TIMED_WAITING".equals(state)) {
                threadInfo.setWaitTime((long) (random.nextDouble() * 10000));
            }
            if ("BLOCKED".equals(state)) {
                threadInfo.setBlockedTime((long) (random.nextDouble() * 5000));
            }

            // 设置当前执行的类和方法
            threadInfo.setCurrentClass(COMMON_CLASSES[random.nextInt(COMMON_CLASSES.length)]);
            threadInfo.setCurrentMethod(COMMON_METHODS[random.nextInt(COMMON_METHODS.length)]);

            threadInfoRepository.save(threadInfo);

            // 生成堆栈信息
            generateThreadStack(processId, threadInfo.getThreadId(), state);
        }

        log.info("Generated {} mock threads for process {}", threadCount, processId);
    }

    /**
     * 生成线程堆栈
     */
    private void generateThreadStack(Long processId, Long threadId, String state) {
        int stackDepth = random.nextInt(20) + 5; // 5-25层堆栈

        for (int depth = 0; depth < stackDepth; depth++) {
            ThreadStack stack = new ThreadStack();
            stack.setProcessId(processId);
            stack.setThreadId(threadId);
            stack.setDepth(depth);
            stack.setClassName(COMMON_CLASSES[random.nextInt(COMMON_CLASSES.length)]);
            stack.setMethodName(COMMON_METHODS[random.nextInt(COMMON_METHODS.length)]);
            stack.setFileName(generateFileName());
            stack.setLineNumber(random.nextInt(1000));
            stack.setNativeMethod(random.nextBoolean());
            stack.setStackTrace(generateStackTrace(stack));

            threadStackRepository.save(stack);
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName() {
        String[] files = {
            "Thread.java",
            "ThreadPoolExecutor.java",
            "AbstractQueuedSynchronizer.java",
            "Service.java",
            "Controller.java",
            "HashMap.java",
            "LinkedBlockingQueue.java"
        };
        return files[random.nextInt(files.length)];
    }

    /**
     * 生成堆栈信息字符串
     */
    private String generateStackTrace(ThreadStack stack) {
        return String.format("at %s.%s(%s:%d)",
            stack.getClassName(),
            stack.getMethodName(),
            stack.getFileName(),
            stack.getLineNumber());
    }

    /**
     * 分析线程状态统计
     */
    public Map<String, Object> analyzeThreadStats(Long processId) {
        List<ThreadInfo> threads = threadInfoRepository.findLatestByProcessId(processId);

        Map<String, Long> stateCount = new HashMap<>();
        stateCount.put("RUNNABLE", 0L);
        stateCount.put("WAITING", 0L);
        stateCount.put("TIMED_WAITING", 0L);
        stateCount.put("BLOCKED", 0L);

        long totalCpuTime = 0;
        long totalWaitTime = 0;
        long totalBlockedTime = 0;
        int aliveCount = 0;
        int daemonCount = 0;

        for (ThreadInfo thread : threads) {
            String state = thread.getState();
            if (state != null) {
                stateCount.put(state, stateCount.getOrDefault(state, 0L) + 1);
            }

            totalCpuTime += thread.getCpuTime() != null ? thread.getCpuTime() : 0;
            totalWaitTime += thread.getWaitTime() != null ? thread.getWaitTime() : 0;
            totalBlockedTime += thread.getBlockedTime() != null ? thread.getBlockedTime() : 0;

            if (Boolean.TRUE.equals(thread.getAlive())) {
                aliveCount++;
            }
            if (Boolean.TRUE.equals(thread.getDaemon())) {
                daemonCount++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", (long) threads.size());
        stats.put("alive", (long) aliveCount);
        stats.put("daemon", (long) daemonCount);
        stats.put("stateCount", stateCount);
        stats.put("totalCpuTime", totalCpuTime);
        stats.put("totalWaitTime", totalWaitTime);
        stats.put("totalBlockedTime", totalBlockedTime);
        stats.put("avgCpuTime", threads.isEmpty() ? 0 : totalCpuTime / threads.size());

        return stats;
    }

    /**
     * 生成具有热点模式的测试线程数据
     * 用于测试AI热点分析功能
     */
    public void generateHotspotMockData(Long processId) {
        ProcessInfo process = processInfoRepository.findById(processId);
        if (process == null || !"running".equals(process.getStatus())) {
            log.warn("进程 {} 不存在或未运行，无法生成热点测试数据", processId);
            return;
        }

        // 只清除当前进程的旧数据
        log.info("清除进程 {} 的旧线程数据", processId);
        clearProcessThreadData(processId);

        int threadCount = 30; // 生成30个线程

        // 根据进程类型定义不同的热点模式
        HotspotPattern[] hotspots;

        if ("database".equals(process.getType())) {
            // MySQL数据库特定的热点模式
            hotspots = new HotspotPattern[] {
                new HotspotPattern("com.mysql.cj.jdbc.MysqlIO", "sendCommand", 28),      // 网络IO
                new HotspotPattern("com.mysql.cj.jdbc.ClientPreparedStatement", "executeQuery", 25), // 查询性能
                new HotspotPattern("com.mysql.cj.jdbc.ConnectionImpl", "isValid", 20),    // 连接检查
                new HotspotPattern("com.zaxxer.hikari.pool.HikariPool", "getConnection", 22), // 连接池等待
                new HotspotPattern("com.mysql.cj.protocol.a.NativeProtocol", "read", 18),     // 读取数据
                new HotspotPattern("com.mysql.cj.jdbc.MysqlIO", "read", 15),                // IO阻塞
                new HotspotPattern("org.springframework.jdbc.core.JdbcTemplate", "query", 12), // JDBC查询
                new HotspotPattern("com.zaxxer.hikari.HikariDataSource", "getConnection", 10), // 连接获取
                new HotspotPattern("com.mysql.cj.jdbc.MysqlIO", "write", 8),                // 写入数据
                new HotspotPattern("org.apache.ibatis.executor.BaseExecutor", "query", 6)      // MyBatis查询
            };
            log.info("使用MySQL数据库特定的热点模式");
        } else {
            // Java应用的标准热点模式
            hotspots = new HotspotPattern[] {
                new HotspotPattern("java.util.HashMap", "put", 25),      // 并发安全问题
                new HotspotPattern("java.util.concurrent.ThreadPoolExecutor", "getTask", 20), // 线程池问题
                new HotspotPattern("java.lang.Thread", "sleep", 15),      // 性能问题
                new HotspotPattern("java.lang.Object", "wait", 12),       // 锁等待
                new HotspotPattern("java.util.LinkedBlockingQueue", "take", 18), // 队列
                new HotspotPattern("java.util.concurrent.locks.ReentrantLock", "lock", 10), // 锁竞争
                new HotspotPattern("com.mysql.jdbc.PreparedStatement", "executeQuery", 8), // 数据库
                new HotspotPattern("java.io.FileInputStream", "read", 7), // IO操作
                new HotspotPattern("java.util.ArrayList", "add", 6),       // 并发安全
                new HotspotPattern("org.springframework.web.servlet.DispatcherServlet", "doService", 14) // HTTP
            };
            log.info("使用Java应用标准热点模式");
        }

        int totalHotspotCount = 0;
        for (HotspotPattern pattern : hotspots) {
            totalHotspotCount += pattern.count;
        }

        for (int i = 0; i < threadCount; i++) {
            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.setProcessId(processId);
            threadInfo.setThreadId((long) (1000 + i));

            // 生成线程名称
            String[] threadTypes = {
                "pool-" + i + "-thread",
                "worker-" + i,
                "async-task-" + i,
                "http-nio-" + (8080 + i) + "-exec-" + i,
                "scheduler-thread-" + i
            };
            threadInfo.setThreadName(threadTypes[random.nextInt(threadTypes.length)]);

            // 随机线程状态
            String state = THREAD_STATES[random.nextInt(THREAD_STATES.length)];
            threadInfo.setState(state);

            // 优先级 1-10
            threadInfo.setPriority(random.nextInt(10) + 1);

            // 随机设置布尔属性
            threadInfo.setDaemon(random.nextBoolean());
            threadInfo.setAlive(true);
            threadInfo.setInterrupted(false);

            // CPU时间
            threadInfo.setCpuTime((long) (random.nextDouble() * 100000));
            threadInfo.setUserTime((long) (random.nextDouble() * 100000));

            // 等待和阻塞时间
            if ("WAITING".equals(state) || "TIMED_WAITING".equals(state)) {
                threadInfo.setWaitTime((long) (random.nextDouble() * 10000));
            }
            if ("BLOCKED".equals(state)) {
                threadInfo.setBlockedTime((long) (random.nextDouble() * 5000));
            }

            // 设置当前执行的类和方法
            threadInfo.setCurrentClass(COMMON_CLASSES[random.nextInt(COMMON_CLASSES.length)]);
            threadInfo.setCurrentMethod(COMMON_METHODS[random.nextInt(COMMON_METHODS.length)]);

            threadInfoRepository.save(threadInfo);

            // 生成具有热点模式的堆栈信息
            generateHotspotThreadStack(processId, threadInfo.getThreadId(), state, hotspots);
        }

        log.info("生成了 {} 个具有热点模式的测试线程", threadCount);
    }

    /**
     * 生成具有热点模式的线程堆栈
     */
    private void generateHotspotThreadStack(Long processId, Long threadId, String state, HotspotPattern[] hotspots) {
        // 根据线程ID分配热点，确保每个热点都会出现
        int hotspotIndex = (int) (threadId % hotspots.length);
        HotspotPattern hotspot = hotspots[hotspotIndex];

        int stackDepth = random.nextInt(15) + 8; // 8-22层堆栈

        for (int depth = 0; depth < stackDepth; depth++) {
            ThreadStack stack = new ThreadStack();
            stack.setProcessId(processId);
            stack.setThreadId(threadId);
            stack.setDepth(depth);

            // 在堆栈底部或随机位置插入热点方法
            if (depth == stackDepth - 1 || (depth > 3 && random.nextDouble() < 0.3)) {
                stack.setClassName(hotspot.className);
                stack.setMethodName(hotspot.methodName);
            } else {
                // 随机选择其他类和方法
                stack.setClassName(COMMON_CLASSES[random.nextInt(COMMON_CLASSES.length)]);
                stack.setMethodName(COMMON_METHODS[random.nextInt(COMMON_METHODS.length)]);
            }

            stack.setFileName(generateFileName());
            stack.setLineNumber(random.nextInt(1000));
            stack.setNativeMethod(random.nextBoolean());
            stack.setStackTrace(generateStackTrace(stack));

            threadStackRepository.save(stack);
        }
    }

    /**
     * 热点模式（内部类）
     */
    private static class HotspotPattern {
        String className;
        String methodName;
        int count;

        HotspotPattern(String className, String methodName, int count) {
            this.className = className;
            this.methodName = methodName;
            this.count = count;
        }
    }
}
