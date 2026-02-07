# 线程热点AI分析功能 - 测试说明

## 功能概述
本功能通过AI分析线程堆栈，自动识别性能热点和潜在问题，提供优化建议。

## 快速测试步骤

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 生成热点测试数据

#### 方式A：使用curl命令
```bash
# 查看进程列表，获取进程ID
curl http://localhost:8080/api/processes

# 假设进程ID为1，生成热点测试数据
curl -X POST http://localhost:8080/api/processes/1/threads/collect-hotspot
```

#### 方式B：使用浏览器访问
1. 打开浏览器访问 `http://localhost:8080/process-monitor.html`
2. 找到一个运行中的进程（如ID=1）
3. 点击"采集数据"按钮
4. 在开发者工具Console中执行：
```javascript
fetch('http://localhost:8080/api/processes/1/threads/collect-hotspot', {
    method: 'POST'
}).then(r => r.json()).then(console.log);
```

### 3. 执行热点分析

#### 方式A：使用curl命令
```bash
curl -X POST http://localhost:8080/api/processes/1/threads/analyze
```

#### 方式B：使用浏览器访问
在开发者工具Console中执行：
```javascript
fetch('http://localhost:8080/api/processes/1/threads/analyze', {
    method: 'POST'
}).then(r => r.json()).then(data => console.log(data));
```

## 预期结果示例

```json
{
  "code": 200,
  "message": "分析成功",
  "data": {
    "processId": 1,
    "analysisTime": "2026-02-06 10:35:00",
    "totalThreads": 30,
    "topHotspots": [
      {
        "className": "java.util.HashMap",
        "methodName": "put",
        "occurrenceCount": 25,
        "issueType": "并发安全",
        "suggestion": "建议使用ConcurrentHashMap或添加synchronized保护",
        "severity": 5
      },
      {
        "className": "java.util.concurrent.ThreadPoolExecutor",
        "methodName": "getTask",
        "occurrenceCount": 20,
        "issueType": "线程池",
        "suggestion": "线程池等待队列较长，考虑增加核心线程数或优化任务",
        "severity": 3
      },
      {
        "className": "java.lang.Thread",
        "methodName": "sleep",
        "occurrenceCount": 15,
        "issueType": "性能优化",
        "suggestion": "频繁的sleep调用影响响应时间，考虑使用事件驱动或异步",
        "severity": 3
      },
      {
        "className": "java.util.LinkedBlockingQueue",
        "methodName": "take",
        "occurrenceCount": 18,
        "issueType": "队列",
        "suggestion": "队列操作频繁，检查队列大小和消费者性能",
        "severity": 2
      },
      {
        "className": "java.lang.Object",
        "methodName": "wait",
        "occurrenceCount": 12,
        "issueType": "锁等待",
        "suggestion": "存在大量wait操作，检查锁持有时间和死锁风险",
        "severity": 4
      },
      {
        "className": "java.util.concurrent.locks.ReentrantLock",
        "methodName": "lock",
        "occurrenceCount": 10,
        "issueType": "锁竞争",
        "suggestion": "存在锁竞争，考虑使用读写锁或减小锁粒度",
        "severity": 4
      },
      {
        "className": "org.springframework.web.servlet.DispatcherServlet",
        "methodName": "doService",
        "occurrenceCount": 14,
        "issueType": "HTTP请求",
        "suggestion": "HTTP请求处理较慢，考虑缓存或异步处理",
        "severity": 2
      },
      {
        "className": "com.mysql.jdbc.PreparedStatement",
        "methodName": "executeQuery",
        "occurrenceCount": 8,
        "issueType": "数据库",
        "suggestion": "数据库操作频繁，检查SQL性能和连接池配置",
        "severity": 3
      },
      {
        "className": "java.io.FileInputStream",
        "methodName": "read",
        "occurrenceCount": 7,
        "issueType": "IO操作",
        "suggestion": "IO操作频繁，建议使用NIO或异步IO",
        "severity": 2
      },
      {
        "className": "java.util.ArrayList",
        "methodName": "add",
        "occurrenceCount": 6,
        "issueType": "并发安全",
        "suggestion": "建议使用ConcurrentHashMap或添加synchronized保护",
        "severity": 5
      }
    ],
    "summary": "检测到10个热点方法，其中3个高优先级问题，2个中优先级问题。健康评分：45分（需关注）",
    "healthScore": 45
  }
}
```

## API接口说明

### 1. 生成热点测试数据
```
POST /api/processes/{processId}/threads/collect-hotspot
```
- 生成包含各种热点模式的线程数据
- 自动生成30个线程
- 包含10种不同的热点模式

### 2. 执行热点分析
```
POST /api/processes/{processId}/threads/analyze
```
- 返回Top 10热点方法
- 包含问题类型、优化建议、严重级别
- 提供健康评分和摘要

## 热点模式说明

测试数据包含以下热点模式：

1. **并发安全问题**（严重级别5）
   - HashMap.put - 多线程环境不安全
   - ArrayList.add - 非线程安全集合

2. **锁等待/竞争**（严重级别4-5）
   - Object.wait - 锁等待
   - ReentrantLock.lock - 锁竞争

3. **线程池问题**（严重级别3）
   - ThreadPoolExecutor.getTask - 线程池配置
   - LinkedBlockingQueue.take - 队列

4. **性能优化**（严重级别3）
   - Thread.sleep - 性能问题
   - DispatcherServlet.doService - HTTP请求

5. **数据库/IO**（严重级别2-3）
   - PreparedStatement.executeQuery - SQL性能
   - FileInputStream.read - IO操作

## 集成到前端（可选）

在 `process-monitor.html` 的线程监控弹窗中添加：

```javascript
async function analyzeHotspots() {
    try {
        const response = await fetch(`/api/processes/${currentProcessId}/threads/analyze`, {
            method: 'POST'
        });
        const result = await response.json();

        if (result.code === 200) {
            showHotspotDialog(result.data);
        } else {
            alert('分析失败：' + result.message);
        }
    } catch (error) {
        alert('分析失败：' + error.message);
    }
}

function showHotspotDialog(analysis) {
    const html = `
        <div class="hotspot-dialog">
            <h3>🔍 线程热点分析</h3>
            <p><strong>摘要：</strong>${analysis.summary}</p>
            <p><strong>健康评分：</strong> ${analysis.healthScore}/100</p>
            <table class="hotspot-table">
                <thead>
                    <tr>
                        <th>类名</th>
                        <th>方法</th>
                        <th>次数</th>
                        <th>问题</th>
                        <th>严重度</th>
                        <th>建议</th>
                    </tr>
                </thead>
                <tbody>
                    ${analysis.topHotspots.map(h => `
                        <tr>
                            <td>${h.className}</td>
                            <td>${h.methodName}</td>
                            <td>${h.occurrenceCount}</td>
                            <td>${h.issueType}</td>
                            <td>${'⭐'.repeat(h.severity)}</td>
                            <td>${h.suggestion}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;

    showDialog(html);
}
```

添加按钮：
```html
<button onclick="analyzeHotspots()">🔍 AI热点分析</button>
<button onclick="generateHotspotData()">📊 生成测试数据</button>
```

## 技术细节

### 1. 数据模型
- `HotspotMethod`: 热点方法信息
- `ThreadHotspotAnalysis`: 分析结果

### 2. 分析逻辑
1. 统计所有线程堆栈中的类+方法出现次数
2. 按出现次数排序，取Top 10
3. 根据类名和方法名判断问题类型
4. 生成优化建议
5. 评估严重级别（1-5）
6. 计算健康评分（0-100）

### 3. 严重级别评估
- 级别5: 并发安全、死锁风险
- 级别4: 锁等待、锁竞争
- 级别3: 线程池、数据库、性能
- 级别2: IO操作、网络
- 级别1: 常规调用

## 扩展建议

### 1. 集成LLM
可以集成OpenAI或其他大语言模型，生成更智能的建议：

```java
@Value("${openai.api-key}")
private String openaiApiKey;

public String generateLLMSuggestion(HotspotMethod hotspot) {
    String prompt = String.format("""
        以下方法在堆栈中出现 %d 次：
        类：%s
        方法：%s

        请给出简短的优化建议（不超过50字）
        """, hotspot.getOccurrenceCount(),
        hotspot.getClassName(),
        hotspot.getMethodName());

    // 调用LLM API
    return callOpenAI(prompt);
}
```

### 2. 历史趋势分析
存储历史分析结果，对比识别持续性问题：

```java
public void saveAnalysis(ThreadHotspotAnalysis analysis) {
    // 存储到数据库
    hotspotAnalysisRepository.save(analysis);
}

public List<Trend> getTrend(Long processId, int days) {
    // 返回历史趋势
}
```

### 3. 实时监控
使用WebSocket推送实时分析结果：

```java
@Scheduled(fixedRate = 60000)
public void scheduledAnalysis() {
    // 每分钟分析一次
    // 通过WebSocket推送结果
}
```

## 注意事项

1. 测试数据是模拟的，生产环境需要接入真实线程dump
2. 建议定期分析，而非实时分析（避免性能影响）
3. 高严重级别问题优先处理
4. 健康评分低于60需要关注

## 常见问题

### Q: 为什么健康评分很低？
A: 因为测试数据故意设置了多种热点问题，用于演示功能。真实环境通常不会这么低。

### Q: 如何接入真实线程数据？
A: 使用Java的`ThreadMXBean`获取真实线程dump，然后调用分析API。

### Q: 支持哪些Java版本？
A: JDK 8及以上版本都支持。

### Q: 性能影响大吗？
A: 不大。分析是基于已采集的堆栈数据，不影响生产运行。
