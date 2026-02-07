function formatDecimal(num, digits = 2) {
    if (num == null) return "-";
    return num.toFixed(digits);
}

function getResourceClass(percentage) {
    if (percentage >= 80) return "high";
    if (percentage >= 60) return "medium";
    return "low";
}

async function loadServers() {
    try {
        const servers = await fetchJSON("/api/servers");
        const serverSelect = document.getElementById("process-server");
        const filterSelect = document.getElementById("filter-server");

        // 清空选项（保留第一个默认选项）
        serverSelect.innerHTML = '<option value="">请选择服务器</option>';
        filterSelect.innerHTML = '<option value="">所有服务器</option>';

        if (servers && servers.length > 0) {
            servers.forEach(server => {
                const option1 = document.createElement("option");
                option1.value = server.id;
                option1.textContent = `${server.name} (${server.ip})`;
                serverSelect.appendChild(option1);

                const option2 = document.createElement("option");
                option2.value = server.id;
                option2.textContent = `${server.name} (${server.ip})`;
                filterSelect.appendChild(option2);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadStats() {
    try {
        const stats = await fetchJSON("/api/processes/stats/summary");
        document.getElementById("stat-total").textContent = stats.total || 0;
        document.getElementById("stat-running").textContent = stats.running || 0;
        document.getElementById("stat-stopped").textContent = stats.stopped || 0;
        document.getElementById("stat-error").textContent = stats.error || 0;
    } catch (e) {
        console.error(e);
    }
}

async function loadProcesses() {
    try {
        const serverId = document.getElementById("filter-server").value;
        const status = document.getElementById("filter-status").value;

        let url = "/api/processes";
        const params = new URLSearchParams();
        if (serverId) params.append("serverId", serverId);
        if (status) params.append("status", status);
        if (params.toString()) url += "?" + params.toString();

        const processes = await fetchJSON(url);
        const processList = document.getElementById("process-list");
        const emptyState = document.getElementById("empty-state");

        if (!processes || processes.length === 0) {
            processList.innerHTML = "";
            emptyState.style.display = "block";
            return;
        }

        emptyState.style.display = "none";
        processList.innerHTML = "";

        for (const process of processes) {
            // 获取最新资源数据，忽略404错误（停止的进程可能没有资源数据）
            let resource = null;
            try {
                resource = await fetchJSON(`/api/processes/${process.id}/resources/latest`);
            } catch (e) {
                // 忽略404错误，资源数据不存在是正常情况
                if (!e.message.includes('404')) {
                    console.warn(`获取进程${process.id}资源数据失败:`, e.message);
                }
            }

            const card = document.createElement("div");
            card.className = "process-card";

            const statusClass = process.status || "stopped";

            card.innerHTML = `
                <div class="process-header">
                    <h3>${process.name}</h3>
                    <span class="status-badge ${statusClass}">
                        ${process.status === "running" ? "运行中" : 
                          process.status === "stopped" ? "已停止" : "异常"}
                    </span>
                </div>
                <div class="process-info">
                    <div class="label">PID: <span class="value">${process.pid || "-"}</span></div>
                    <div class="label">类型: <span class="value">${process.type || "-"}</span></div>
                    <div class="label">端口: <span class="value">${process.ports || "-"}</span></div>
                </div>
                ${resource ? `
                <div class="resource-bar">
                    <div class="resource-bar-label">
                        <span>CPU</span>
                        <span>${formatDecimal(resource.cpuUsage)}%</span>
                    </div>
                    <div class="resource-bar-track">
                        <div class="resource-bar-fill ${getResourceClass(resource.cpuUsage * 3)}" style="width: ${Math.min(resource.cpuUsage * 3, 100)}%"></div>
                    </div>
                </div>
                <div class="resource-bar">
                    <div class="resource-bar-label">
                        <span>内存</span>
                        <span>${formatDecimal(resource.memoryUsage)} MB (${formatDecimal(resource.memoryPercent)}%)</span>
                    </div>
                    <div class="resource-bar-track">
                        <div class="resource-bar-fill ${getResourceClass(resource.memoryPercent * 5)}" style="width: ${Math.min(resource.memoryPercent * 5, 100)}%"></div>
                    </div>
                </div>
                <div class="process-info" style="margin-top: 10px;">
                    <div class="label">线程: <span class="value">${resource.threadCount || "-"}</span></div>
                    <div class="label">句柄: <span class="value">${resource.handleCount || "-"}</span></div>
                    <div class="label">运行时间: <span class="value">${formatUptime(resource.uptime)}</span></div>
                </div>
                ` : '<p style="color: #999; font-size: 13px;">暂无资源数据</p>'}
                <div class="process-actions">
                    <button onclick="showDetail(${process.id})">详情</button>
                    <button onclick="showThreads(${process.id})" ${process.status !== "running" ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''}>线程监控</button>
                    <button onclick="collectResources(${process.id})">采集数据</button>
                    ${process.status === "running" ? `
                        <button onclick="stopProcess(${process.id})">停止</button>
                        <button onclick="restartProcess(${process.id})">重启</button>
                    ` : `
                        <button onclick="startProcess(${process.id})">启动</button>
                    `}
                    <button onclick="deleteProcess(${process.id})" style="background: #f5222d;">删除</button>
                </div>
            `;

            processList.appendChild(card);
        }
    } catch (e) {
        console.error(e);
        alert("加载进程列表失败：" + e.message);
    }
}

function showAddProcessModal() {
    document.getElementById("add-process-modal").classList.add("active");
}

function hideAddProcessModal() {
    document.getElementById("add-process-modal").classList.remove("active");
    document.getElementById("add-process-form").reset();
}

async function handleAddProcess(event) {
    event.preventDefault();

    const processInfo = {
        name: document.getElementById("process-name").value.trim(),
        serverId: document.getElementById("process-server").value,
        type: document.getElementById("process-type").value,
        ports: document.getElementById("process-ports").value.trim(),
        startCommand: document.getElementById("process-start-cmd").value.trim(),
        stopCommand: document.getElementById("process-stop-cmd").value.trim(),
        description: document.getElementById("process-desc").value.trim(),
        autoStart: document.getElementById("process-autostart").checked,
        status: "stopped"
    };

    try {
        await fetchJSON("/api/processes", {
            method: "POST",
            body: JSON.stringify(processInfo)
        });
        hideAddProcessModal();
        await loadProcesses();
        await loadStats();
    } catch (e) {
        console.error(e);
        alert("添加进程失败：" + e.message);
    }
}

async function startProcess(id) {
    if (!confirm("确定要启动该进程吗？")) return;

    try {
        const result = await fetchJSON(`/api/processes/${id}/start`, {
            method: "POST"
        });
        if (result.success) {
            await loadProcesses();
            await loadStats();
        } else {
            alert(result.message);
        }
    } catch (e) {
        console.error(e);
        alert("启动进程失败：" + e.message);
    }
}

async function stopProcess(id) {
    if (!confirm("确定要停止该进程吗？")) return;

    try {
        const result = await fetchJSON(`/api/processes/${id}/stop`, {
            method: "POST"
        });
        if (result.success) {
            await loadProcesses();
            await loadStats();
        } else {
            alert(result.message);
        }
    } catch (e) {
        console.error(e);
        alert("停止进程失败：" + e.message);
    }
}

async function restartProcess(id) {
    if (!confirm("确定要重启该进程吗？")) return;

    try {
        const result = await fetchJSON(`/api/processes/${id}/restart`, {
            method: "POST"
        });
        if (result.success) {
            await loadProcesses();
            await loadStats();
        } else {
            alert(result.message);
        }
    } catch (e) {
        console.error(e);
        alert("重启进程失败：" + e.message);
    }
}

async function deleteProcess(id) {
    if (!confirm("确定要删除该进程吗？")) return;

    try {
        await fetchJSON(`/api/processes/${id}`, {
            method: "DELETE"
        });
        await loadProcesses();
        await loadStats();
    } catch (e) {
        console.error(e);
        alert("删除进程失败：" + e.message);
    }
}

async function collectResources(id) {
    try {
        await fetchJSON(`/api/processes/${id}/resources/collect`, {
            method: "POST"
        });
        await loadProcesses();
    } catch (e) {
        console.error(e);
        alert("采集数据失败：" + e.message);
    }
}

async function showDetail(id) {
    try {
        const process = await fetchJSON(`/api/processes/${id}`);
        const resources = await fetchJSON(`/api/processes/${id}/resources?limit=100`);

        document.getElementById("detail-process-name").textContent = process.name;

        const content = document.getElementById("process-detail-content");
        content.innerHTML = `
            <div class="detail-section">
                <h3>基本信息</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">进程名称</span>
                        <span class="detail-value">${process.name}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">PID</span>
                        <span class="detail-value">${process.pid || "-"}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">类型</span>
                        <span class="detail-value">${process.type || "-"}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">状态</span>
                        <span class="detail-value ${process.status}">${process.status === "running" ? "运行中" : process.status === "stopped" ? "已停止" : "异常"}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">端口</span>
                        <span class="detail-value">${process.ports || "-"}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">自动启动</span>
                        <span class="detail-value">${process.autoStart ? "是" : "否"}</span>
                    </div>
                </div>
            </div>
            <div class="detail-section">
                <h3>命令信息</h3>
                <div class="detail-grid">
                    <div class="detail-item full-width">
                        <span class="detail-label">启动命令</span>
                        <span class="detail-value">${process.startCommand || "-"}</span>
                    </div>
                    <div class="detail-item full-width">
                        <span class="detail-label">停止命令</span>
                        <span class="detail-value">${process.stopCommand || "-"}</span>
                    </div>
                </div>
            </div>
            ${process.description ? `
            <div class="detail-section">
                <h3>描述</h3>
                <p>${process.description}</p>
            </div>
            ` : ''}
            <div class="detail-section">
                <h3>资源监控</h3>
                <div id="detail-chart" style="height: 300px; margin: 20px 0;"></div>
            </div>
        `;

        document.getElementById("process-detail-modal").classList.add("active");

        // 绘制图表
        if (resources && resources.length > 0) {
            drawChart(resources);
        } else {
            document.getElementById("detail-chart").innerHTML = '<p style="text-align: center; color: #999;">暂无数据</p>';
        }
    } catch (e) {
        console.error(e);
        alert("加载进程详情失败：" + e.message);
    }
}

function hideDetailModal() {
    document.getElementById("process-detail-modal").classList.remove("active");
}

function drawChart(resources) {
    const chart = document.getElementById("detail-chart");

    // 准备数据
    const labels = resources.map(r => {
        const d = new Date(r.timestamp);
        return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    }).reverse();

    const cpuData = resources.map(r => r.cpuUsage).reverse();
    const memoryData = resources.map(r => r.memoryUsage).reverse();

    // 计算最大值用于缩放
    const maxCpu = Math.max(...cpuData, 1);
    const maxMemory = Math.max(...memoryData, 1);

    // 绘制简单的SVG图表
    const width = chart.clientWidth || 600;
    const height = 300;
    const padding = 40;
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;

    let svg = `<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">`;

    // 绘制网格线
    for (let i = 0; i <= 5; i++) {
        const y = padding + (chartHeight / 5) * i;
        svg += `<line x1="${padding}" y1="${y}" x2="${width - padding}" y2="${y}" stroke="#e0e0e0" stroke-width="1"/>`;
        const value = maxMemory - (maxMemory / 5) * i;
        svg += `<text x="${padding - 10}" y="${y + 4}" font-size="10" text-anchor="end" fill="#999">${value.toFixed(0)}</text>`;
    }

    // 绘制CPU线
    let cpuPath = "";
    const stepX = chartWidth / (cpuData.length - 1);
    cpuData.forEach((value, i) => {
        const x = padding + i * stepX;
        const y = padding + chartHeight - (value / maxCpu) * chartHeight;
        cpuPath += `${i === 0 ? "M" : "L"} ${x} ${y}`;
    });
    svg += `<path d="${cpuPath}" fill="none" stroke="#667eea" stroke-width="2"/>`;

    // 绘制内存线
    let memoryPath = "";
    memoryData.forEach((value, i) => {
        const x = padding + i * stepX;
        const y = padding + chartHeight - (value / maxMemory) * chartHeight;
        memoryPath += `${i === 0 ? "M" : "L"} ${x} ${y}`;
    });
    svg += `<path d="${memoryPath}" fill="none" stroke="#764ba2" stroke-width="2"/>`;

    // 绘制X轴标签
    labels.forEach((label, i) => {
        if (i % 5 === 0 || i === labels.length - 1) {
            const x = padding + i * stepX;
            svg += `<text x="${x}" y="${height - 10}" font-size="10" text-anchor="middle" fill="#666">${label}</text>`;
        }
    });

    // 图例
    svg += `<rect x="${padding + 10}" y="10" width="12" height="12" fill="#667eea"/>`;
    svg += `<text x="${padding + 28}" y="20" font-size="12" fill="#333">CPU使用率 (%)</text>`;
    svg += `<rect x="${padding + 150}" y="10" width="12" height="12" fill="#764ba2"/>`;
    svg += `<text x="${padding + 168}" y="20" font-size="12" fill="#333">内存使用量 (MB)</text>`;

    svg += "</svg>";
    chart.innerHTML = svg;
}

function formatUptime(seconds) {
    if (!seconds) return "-";
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) {
        return `${days}天 ${hours}小时`;
    } else if (hours > 0) {
        return `${hours}小时 ${minutes}分钟`;
    } else {
        return `${minutes}分钟`;
    }
}

// 线程监控相关功能
let currentProcessId = null;
let currentThreads = [];

async function showThreads(processId) {
    currentProcessId = processId;

    try {
        const process = await fetchJSON(`/api/processes/${processId}`);
        document.getElementById("thread-list-title").textContent = `${process.name} - 线程监控`;

        await loadThreads();
        await loadThreadStats();

        document.getElementById("thread-list-modal").classList.add("active");
    } catch (e) {
        console.error(e);
        alert("加载线程列表失败：" + e.message);
    }
}

function hideThreadModal() {
    document.getElementById("thread-list-modal").classList.remove("active");
    currentProcessId = null;
    currentThreads = [];
}

async function loadThreads() {
    try {
        const threads = await fetchJSON(`/api/processes/${currentProcessId}/threads`);
        currentThreads = threads;

        const stateFilter = document.getElementById("filter-thread-state").value;
        const filteredThreads = stateFilter
            ? threads.filter(t => t.state === stateFilter)
            : threads;

        renderThreads(filteredThreads);
    } catch (e) {
        console.error(e);
        alert("加载线程失败：" + e.message);
    }
}

function renderThreads(threads) {
    const threadList = document.getElementById("thread-list");

    if (!threads || threads.length === 0) {
        threadList.innerHTML = '<p style="text-align: center; padding: 40px; color: #999;">暂无线程数据，点击"采集数据"开始采集</p>';
        return;
    }

    threadList.innerHTML = '';

    threads.forEach(thread => {
        const stateClass = thread.state || 'UNKNOWN';
        const stateText = getStateText(thread.state);

        const card = document.createElement("div");
        card.className = "thread-card";
        card.innerHTML = `
            <div class="thread-header">
                <div style="flex: 1;">
                    <div class="thread-name">${thread.threadName || 'Unknown'}</div>
                    <div class="thread-id">ID: ${thread.threadId || '-'}</div>
                </div>
                <div style="text-align: right;">
                    <span class="state-badge ${stateClass}">${stateText}</span>
                    <div style="margin-top: 5px; font-size: 12px; color: #666;">
                        优先级: ${thread.priority || '-'} | ${thread.daemon ? '守护' : '用户'}线程
                    </div>
                </div>
            </div>
            <div class="thread-info-grid">
                <div class="thread-info-item">
                    <span class="thread-label">CPU时间</span>
                    <span class="thread-value">${formatTime(thread.cpuTime)}</span>
                </div>
                <div class="thread-info-item">
                    <span class="thread-label">等待时间</span>
                    <span class="thread-value">${formatTime(thread.waitTime)}</span>
                </div>
                <div class="thread-info-item">
                    <span class="thread-label">阻塞时间</span>
                    <span class="thread-value">${formatTime(thread.blockedTime)}</span>
                </div>
                <div class="thread-info-item">
                    <span class="thread-label">当前执行</span>
                    <span class="thread-value">${thread.currentMethod || '-'}()</span>
                </div>
            </div>
            ${thread.currentClass ? `
            <div class="thread-location">
                <span class="thread-location-label">当前位置:</span>
                <span class="thread-location-value">${thread.currentClass}.${thread.currentMethod}(${thread.currentLine ? ':' + thread.currentLine : ''})</span>
            </div>
            ` : ''}
            <div class="thread-actions">
                <button onclick="showThreadStack(${thread.threadId})" style="flex: 1;">查看堆栈</button>
            </div>
        `;

        threadList.appendChild(card);
    });
}

function getStateText(state) {
    const stateMap = {
        'RUNNABLE': '运行中',
        'WAITING': '等待',
        'TIMED_WAITING': '限时等待',
        'BLOCKED': '阻塞',
        'NEW': '新建',
        'TERMINATED': '已终止'
    };
    return stateMap[state] || state;
}

function formatTime(milliseconds) {
    if (!milliseconds || milliseconds === 0) return '-';
    const seconds = milliseconds / 1000;
    if (seconds < 1) return milliseconds + 'ms';
    if (seconds < 60) return seconds.toFixed(2) + 's';
    const minutes = seconds / 60;
    if (minutes < 60) return minutes.toFixed(2) + 'm';
    const hours = minutes / 60;
    return hours.toFixed(2) + 'h';
}

async function loadThreadStats() {
    try {
        const stats = await fetchJSON(`/api/processes/${currentProcessId}/threads/stats`);
        const statsDiv = document.getElementById("thread-stats");

        const stateCount = stats.stateCount || {};
        statsDiv.innerHTML = `
            总线程: ${stats.total} |
            活跃: ${stats.alive} |
            守护: ${stats.daemon} |
            平均CPU: ${formatTime(stats.avgCpuTime)} |
            状态分布: 运行(${stateCount.RUNNABLE || 0}) 等待(${stateCount.WAITING || 0}) 限时等待(${stateCount.TIMED_WAITING || 0}) 阻塞(${stateCount.BLOCKED || 0})
        `;
    } catch (e) {
        console.error(e);
    }
}

async function collectThreadData() {
    try {
        await fetchJSON(`/api/processes/${currentProcessId}/threads/collect`, {
            method: "POST",
            body: JSON.stringify({ threadCount: 20 })
        });
        await loadThreads();
        await loadThreadStats();
    } catch (e) {
        console.error(e);
        alert("采集线程数据失败：" + e.message);
    }
}

async function showThreadStack(threadId) {
    try {
        const stacks = await fetchJSON(`/api/processes/${currentProcessId}/threads/${threadId}/stack`);
        const thread = currentThreads.find(t => t.threadId === threadId);

        document.getElementById("thread-stack-title").textContent =
            `${thread ? thread.threadName : '线程'} - 堆栈信息`;

        const stackContent = document.getElementById("thread-stack-content");

        if (!stacks || stacks.length === 0) {
            stackContent.innerHTML = '<p style="text-align: center; padding: 40px; color: #999;">暂无堆栈信息</p>';
        } else {
            stackContent.innerHTML = `
                <div class="thread-info" style="margin-bottom: 20px;">
                    <div class="label">线程状态: <span class="value">${getStateText(thread.state)}</span></div>
                    <div class="label">优先级: <span class="value">${thread.priority}</span></div>
                    <div class="label">守护线程: <span class="value">${thread.daemon ? '是' : '否'}</span></div>
                    <div class="label">CPU时间: <span class="value">${formatTime(thread.cpuTime)}</span></div>
                </div>
                <div class="stack-container">
                    <h3 style="margin-bottom: 15px; color: #667eea;">调用堆栈</h3>
                    ${stacks.map((stack, index) => `
                        <div class="stack-item" style="margin-left: ${index * 20}px;">
                            <div class="stack-index">#${stack.depth}</div>
                            <div class="stack-content">
                                <div class="stack-class">${stack.className}</div>
                                <div class="stack-method">${stack.methodName}()</div>
                                <div class="stack-file">at ${stack.fileName}:${stack.lineNumber}</div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }

        document.getElementById("thread-stack-modal").classList.add("active");
    } catch (e) {
        console.error(e);
        alert("加载线程堆栈失败：" + e.message);
    }
}

function hideStackModal() {
    document.getElementById("thread-stack-modal").classList.remove("active");
}

function filterThreads() {
    if (currentProcessId) {
        loadThreads();
    }
}

// ========== AI热点分析相关函数 ==========

/**
 * 生成热点测试数据
 */
async function generateHotspotTestData() {
    try {
        const result = await fetchJSON(`/api/processes/${currentProcessId}/threads/collect-hotspot`, {
            method: "POST"
        });
        alert(result.message || "热点测试数据生成成功！");
        await loadThreads();
        await loadThreadStats();
    } catch (e) {
        console.error(e);
        alert("生成测试数据失败：" + e.message);
    }
}

/**
 * 执行AI热点分析
 */
async function analyzeHotspots() {
    if (!currentProcessId) {
        alert("请先选择一个进程");
        return;
    }

    // 显示加载状态
    const modal = document.getElementById("hotspot-analysis-modal");
    const content = document.getElementById("hotspot-analysis-content");
    const loading = document.getElementById("hotspot-loading");
    const empty = document.getElementById("hotspot-empty");

    if (!modal || !content || !loading || !empty) {
        alert("弹窗元素未正确加载，请刷新页面重试");
        return;
    }

    modal.classList.add("active");
    content.style.display = "none";
    loading.style.display = "flex";
    loading.style.justifyContent = "center";
    loading.style.alignItems = "center";
    empty.style.display = "none";

    try {
        // 调用后端分析API
        const result = await fetchJSON(`/api/processes/${currentProcessId}/threads/analyze`, {
            method: "POST"
        });

        console.log("热点分析API响应:", result);

        // 隐藏加载状态
        loading.style.display = "none";

        // 检查响应格式和数据
        if (!result) {
            console.error("API响应为空");
            empty.style.display = "flex";
            empty.style.justifyContent = "center";
            empty.style.alignItems = "center";
            return;
        }

        // 兼容不同的响应格式
        const analysis = result.data || result;

        console.log("分析结果:", analysis);

        // 检查是否有热点数据
        if (!analysis || !analysis.topHotspots || analysis.topHotspots.length === 0) {
            console.log("未检测到热点数据");
            empty.style.display = "flex";
            empty.style.justifyContent = "center";
            empty.style.alignItems = "center";
            return;
        }

        // 显示分析结果
        content.style.display = "flex";

        // 验证必需的元素存在
        const summaryEl = document.getElementById("hotspot-summary");
        const listEl = document.getElementById("hotspot-list");

        if (!summaryEl || !listEl) {
            console.error("热点分析弹窗元素缺失", {
                summary: !!summaryEl,
                list: !!listEl
            });
            alert("热点分析弹窗未正确加载，请按Ctrl+F5强制刷新页面");
            modal.classList.remove("active");
            return;
        }

        // 更新摘要
        summaryEl.textContent =
            `${analysis.summary || ''} | 线程总数: ${analysis.totalThreads || 0} | 分析时间: ${analysis.analysisTime || ''}`;

        // 更新健康评分
        if (analysis.healthScore !== undefined) {
            updateHealthScore(analysis.healthScore);
        }

        // 更新热点方法列表
        renderHotspotTable(analysis.topHotspots);

    } catch (e) {
        console.error("热点分析错误:", e);
        loading.style.display = "none";
        alert("热点分析失败：" + e.message);
        modal.classList.remove("active");
    }
}

/**
 * 更新健康评分显示
 */
function updateHealthScore(score) {
    const valueDiv = document.getElementById("health-score-value");
    const labelDiv = document.getElementById("health-score-label");
    const cardDiv = document.getElementById("health-score-card");

    if (!valueDiv || !labelDiv || !cardDiv) {
        console.error("健康评分元素未找到");
        return;
    }

    valueDiv.textContent = score;

    // 设置评分标签
    if (score >= 80) {
        labelDiv.textContent = "系统运行良好";
        cardDiv.style.background = "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)";
    } else if (score >= 60) {
        labelDiv.textContent = "系统运行一般";
        cardDiv.style.background = "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";
    } else {
        labelDiv.textContent = "系统需关注";
        cardDiv.style.background = "linear-gradient(135deg, #eb3349 0%, #f45c43 100%)";
    }
}

/**
 * 渲染热点方法列表
 */
function renderHotspotTable(hotspots) {
    const list = document.getElementById("hotspot-list");
    if (!list) {
        console.error("hotspot-list element not found");
        return;
    }
    list.innerHTML = "";

    // 更新统计信息
    updateHotspotStats(hotspots);

    if (!hotspots || hotspots.length === 0) {
        return;
    }

    hotspots.forEach((hotspot, index) => {
        const item = document.createElement("div");

        // 根据严重级别设置样式
        let severityClass = "low";
        let borderColor = "#52c41a";
        if (hotspot.severity >= 5) {
            severityClass = "critical";
            borderColor = "#ff4d4f";
        } else if (hotspot.severity >= 4) {
            severityClass = "high";
            borderColor = "#fa8c16";
        } else if (hotspot.severity >= 3) {
            severityClass = "medium";
            borderColor = "#faad14";
        }

        item.className = `hotspot-item ${severityClass}`;

        item.innerHTML = `
            <div class="hotspot-header">
                <div style="display: flex; flex: 1; min-width: 0;">
                    <div class="hotspot-rank">#${index + 1}</div>
                    <div class="hotspot-info">
                        <div class="hotspot-class">${escapeHtml(hotspot.className)}</div>
                        <div class="hotspot-method">${escapeHtml(hotspot.methodName)}()</div>
                        <div class="hotspot-meta">
                            <span class="hotspot-count">调用 ${hotspot.occurrenceCount} 次</span>
                            <span class="hotspot-type">${escapeHtml(hotspot.issueType)}</span>
                            <div class="hotspot-severity">${'⭐'.repeat(hotspot.severity)}</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="hotspot-suggestion">
                <div class="hotspot-suggestion-title">💡 优化建议</div>
                <div class="hotspot-suggestion-content">${escapeHtml(hotspot.suggestion || '暂无建议')}</div>
            </div>
        `;

        list.appendChild(item);
    });
}

/**
 * 更新热点统计信息
 */
function updateHotspotStats(hotspots) {
    try {
        const critical = hotspots.filter(h => h.severity >= 5).length;
        const high = hotspots.filter(h => h.severity === 4).length;
        const medium = hotspots.filter(h => h.severity === 3).length;

        const statCritical = document.getElementById("stat-critical");
        const statHigh = document.getElementById("stat-high");
        const statMedium = document.getElementById("stat-medium");
        const statTotal = document.getElementById("stat-total");

        if (statCritical) statCritical.textContent = critical;
        if (statHigh) statHigh.textContent = high;
        if (statMedium) statMedium.textContent = medium;
        if (statTotal) statTotal.textContent = hotspots.length;
    } catch (e) {
        console.error("Error updating hotspot stats:", e);
    }
}

/**
 * HTML转义，防止XSS攻击
 */
function escapeHtml(text) {
    if (!text) return "";
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 根据严重级别获取颜色
 */
function getSeverityColor(severity) {
    switch (severity) {
        case 5: return "#dc3545"; // 红色
        case 4: return "#fd7e14"; // 橙色
        case 3: return "#ffc107"; // 黄色
        case 2: return "#20c997"; // 青色
        case 1: return "#6c757d"; // 灰色
        default: return "#6c757d";
    }
}

/**
 * 隐藏热点分析弹窗
 */
function hideHotspotModal() {
    const modal = document.getElementById("hotspot-analysis-modal");
    modal.classList.remove("active");

    // 重置弹窗内容状态
    setTimeout(() => {
        const content = document.getElementById("hotspot-analysis-content");
        const loading = document.getElementById("hotspot-loading");
        const empty = document.getElementById("hotspot-empty");
        const list = document.getElementById("hotspot-list");

        if (content) {
            content.style.display = "flex";
            content.scrollTop = 0; // 重置滚动位置
        }
        if (loading) loading.style.display = "none";
        if (empty) empty.style.display = "none";
        if (list) list.innerHTML = ""; // 清空热点列表
    }, 300); // 等待动画完成
}

// 页面加载时初始化
document.addEventListener("DOMContentLoaded", () => {
    loadServers();
    loadStats();
    loadProcesses();

    // 绑定表单提交（确保DOM元素存在）
    const addProcessForm = document.getElementById("add-process-form");
    if (addProcessForm) {
        addProcessForm.addEventListener("submit", handleAddProcess);
    }

    // 定时刷新数据（每30秒）
    setInterval(() => {
        loadProcesses();
        loadStats();
    }, 30000);
});
