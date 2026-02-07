// 获取URL中的服务器ID参数
const urlParams = new URLSearchParams(window.location.search);
const serverId = urlParams.get('id');

let autoRefreshInterval = null;

async function fetchJSON(url, options = {}) {
    const resp = await fetch(url, {
        headers: {
            "Content-Type": "application/json"
        },
        ...options
    });
    if (!resp.ok) {
        const text = await resp.text();
        throw new Error(`请求失败: ${resp.status} ${text}`);
    }
    const contentType = resp.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return resp.json();
    }
    return null;
}

function formatDecimal(num, digits = 2) {
    if (num == null) return "-";
    return num.toFixed(digits);
}

function formatDateTime(timestamp) {
    if (!timestamp) return "-";
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function goBack() {
    // 返回服务器列表页面
    window.location.href = 'server.html';
}

// CSS饼状图 - 使用conic-gradient
function updatePieChart(elementId, value, color) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const emptyColor = '#e0e0e0';
    const gradient = `conic-gradient(${color} 0% ${value}%, ${emptyColor} ${value}% 100%)`;

    element.style.cssText = `
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: ${gradient};
        position: relative;
    `;

    // 添加中心圆环效果
    element.innerHTML = `
        <div style="
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 60px;
            height: 60px;
            background: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            font-weight: bold;
            color: #333;
        ">${formatDecimal(value)}%</div>
    `;
}

// 负载指示器 - 使用CSS圆形进度条
function updateLoadGauge(value) {
    const element = document.getElementById('load-chart');
    if (!element) return;

    const maxLoad = 10;
    const percentage = Math.min((value / maxLoad) * 100, 100);
    const color = percentage > 80 ? '#ee6666' : percentage > 60 ? '#fac858' : '#91cc75';

    element.style.cssText = `
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: conic-gradient(${color} ${percentage}%, #e0e0e0 ${percentage}% 100%);
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
    `;

    element.innerHTML = `
        <div style="
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 70px;
            height: 70px;
            background: white;
            border-radius: 50%;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
        ">
            <span style="font-size: 18px; font-weight: bold; color: #333;">${formatDecimal(value)}</span>
            <span style="font-size: 11px; color: #999;">负载</span>
        </div>
    `;
}

// SVG折线图
function updateLineChart(data, config) {
    const element = document.getElementById(config.elementId);
    if (!element || !data || data.length === 0) {
        if (element) {
            element.innerHTML = '<div style="text-align: center; color: #999; padding: 150px 0;">暂无历史数据</div>';
        }
        return;
    }

    const width = element.clientWidth || 800;
    const height = 400;
    const padding = { top: 30, right: 30, bottom: 50, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    // 获取所有数据的范围
    const allValues = config.series.map(s => data.map(d => d[s.key] || 0)).flat();
    const maxValue = Math.max(...allValues, 100);
    const minValue = 0;

    // 计算点的坐标
    const calculatePoints = (key) => {
        return data.map((d, i) => {
            const x = padding.left + (i / (data.length - 1)) * chartWidth;
            const value = d[key] || 0;
            const y = padding.top + chartHeight - ((value - minValue) / (maxValue - minValue)) * chartHeight;
            return { x, y, value };
        });
    };

    // 生成SVG路径
    const generatePath = (points) => {
        if (points.length === 0) return '';
        let path = `M ${points[0].x} ${points[0].y}`;
        for (let i = 1; i < points.length; i++) {
            path += ` L ${points[i].x} ${points[i].y}`;
        }
        return path;
    };

    // 生成渐变填充区域
    const generateAreaPath = (points) => {
        if (points.length === 0) return '';
        let path = `M ${points[0].x} ${padding.top + chartHeight}`;
        points.forEach(p => {
            path += ` L ${p.x} ${p.y}`;
        });
        path += ` L ${points[points.length - 1].x} ${padding.top + chartHeight}`;
        path += ' Z';
        return path;
    };

    // 生成X轴标签
    const xLabels = data.map((d, i) => {
        const x = padding.left + (i / (data.length - 1)) * chartWidth;
        const text = formatDateTime(d.timestamp).split(' ')[1]; // 只显示时间部分
        return `<text x="${x}" y="${height - padding.bottom + 20}" text-anchor="middle" font-size="10" fill="#666">${text}</text>`;
    });

    // 生成Y轴标签
    const yLabels = [];
    for (let i = 0; i <= 5; i++) {
        const value = minValue + (maxValue - minValue) * (i / 5);
        const y = padding.top + chartHeight - (i / 5) * chartHeight;
        yLabels.push(`<text x="${padding.left - 10}" y="${y + 4}" text-anchor="end" font-size="10" fill="#666">${Math.round(value)}%</text>`);
    }

    // 生成网格线
    const gridLines = [];
    for (let i = 0; i <= 5; i++) {
        const y = padding.top + chartHeight - (i / 5) * chartHeight;
        gridLines.push(`<line x1="${padding.left}" y1="${y}" x2="${width - padding.right}" y2="${y}" stroke="#e0e0e0" stroke-width="1" stroke-dasharray="5,5"/>`);
    }

    // 生成系列
    const seriesElements = config.series.map((s, index) => {
        const points = calculatePoints(s.key);
        const path = generatePath(points);
        const areaPath = generateAreaPath(points);

        return `
            <defs>
                <linearGradient id="gradient${index}" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" style="stop-color:${s.color};stop-opacity:0.3" />
                    <stop offset="100%" style="stop-color:${s.color};stop-opacity:0.05" />
                </linearGradient>
            </defs>
            <path d="${areaPath}" fill="url(#gradient${index})" />
            <path d="${path}" fill="none" stroke="${s.color}" stroke-width="2" />
            ${points.map(p => `<circle cx="${p.x}" cy="${p.y}" r="3" fill="${s.color}" />`).join('')}
        `;
    });

    // 图例
    const legend = config.series.map(s => `
        <g transform="translate(${padding.left + (config.series.indexOf(s) * 100)}, 10)">
            <rect width="12" height="12" fill="${s.color}" rx="2" />
            <text x="18" y="10" font-size="12" fill="#666">${s.name}</text>
        </g>
    `).join('');

    const svg = `
        <svg width="100%" height="${height}" viewBox="0 0 ${width} ${height}" preserveAspectRatio="xMidYMid meet">
            ${gridLines.join('')}
            ${xLabels.join('')}
            ${yLabels.join('')}
            ${seriesElements.join('')}
            ${legend}
        </svg>
    `;

    element.innerHTML = svg;
}

// CSS柱状图
function updateBarChart(data, config) {
    const element = document.getElementById(config.elementId);
    if (!element || !data || data.length === 0) {
        if (element) {
            element.innerHTML = '<div style="text-align: center; color: #999; padding: 100px 0;">暂无历史数据</div>';
        }
        return;
    }

    const width = element.clientWidth || 800;
    const barWidth = Math.max(20, Math.min(40, (width - 100) / data.length / 2 - 10));
    const gap = barWidth / 2;

    // 获取所有数据的最大值
    const allValues = config.series.map(s => data.map(d => d[s.key] || 0)).flat();
    const maxValue = Math.max(...allValues, 1);

    // 生成HTML
    let html = `<div style="display: flex; align-items: flex-end; justify-content: space-around; height: 250px; padding: 0 50px; position: relative; margin-bottom: 20px;">`;

    // 添加Y轴标签
    for (let i = 0; i <= 5; i++) {
        const value = maxValue * (i / 5);
        const bottomPercent = (i / 5) * 100;
        html += `<div style="position: absolute; left: 10px; bottom: ${bottomPercent}%; font-size: 11px; color: #999;">${formatDecimal(value)}</div>`;
    }

    // 生成柱状图
    data.forEach((d, i) => {
        const timeLabel = formatDateTime(d.timestamp).split(' ')[1];

        html += `
            <div style="display: flex; align-items: flex-end; gap: 4px; position: relative; height: 250px;">
                ${config.series.map(s => {
                    const value = d[s.key] || 0;
                    const heightPercent = maxValue > 0 ? (value / maxValue) * 100 : 0;
                    return `
                        <div style="
                            width: ${barWidth}px;
                            height: ${heightPercent}%;
                            background: ${s.color};
                            border-radius: 4px 4px 0 0;
                            transition: height 0.3s ease;
                            position: relative;
                            min-height: ${value > 0 ? '4px' : '0'};
                        " title="${s.name}: ${formatDecimal(value)} MB/s"></div>
                    `;
                }).join('')}
                <div style="
                    position: absolute;
                    bottom: -25px;
                    left: 50%;
                    transform: translateX(-50%);
                    font-size: 10px;
                    color: #666;
                    white-space: nowrap;
                ">${timeLabel}</div>
            </div>
        `;
    });

    html += '</div>';

    // 添加图例
    html += '<div style="display: flex; justify-content: center; gap: 20px; margin-top: 10px;">';
    config.series.forEach(s => {
        html += `
            <div style="display: flex; align-items: center; gap: 8px;">
                <div style="width: 12px; height: 12px; background: ${s.color}; border-radius: 2px;"></div>
                <span style="font-size: 12px; color: #666;">${s.name}</span>
            </div>
        `;
    });
    html += '</div>';

    element.innerHTML = html;
}

async function loadServerInfo() {
    try {
        const server = await fetchJSON(`/api/servers/${serverId}`);
        if (!server) {
            alert('未找到服务器信息');
            goBack();
            return;
        }

        document.getElementById('server-name').textContent = server.name;

        const statusClass = server.status || 'offline';
        const statusText = server.status === 'online' ? '在线' : '离线';

        const infoHtml = `
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">状态:</span>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">IP地址:</span>
                    <span class="detail-value">${server.ip || '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">服务器类型:</span>
                    <span class="detail-value">${server.type || '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">环境:</span>
                    <span class="detail-value">${server.env || '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">CPU核心数:</span>
                    <span class="detail-value">${server.cpuCores || '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">总内存:</span>
                    <span class="detail-value">${server.totalMemory || '-'} GB</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">总磁盘:</span>
                    <span class="detail-value">${server.totalDisk || '-'} GB</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">描述:</span>
                    <span class="detail-value">${server.description || '-'}</span>
                </div>
                <div class="detail-item" style="grid-column: span 2;">
                    <span class="detail-label">创建时间:</span>
                    <span class="detail-value">${server.createdAt ? formatDateTime(server.createdAt) : '-'}</span>
                </div>
            </div>
        `;

        document.getElementById('server-detail-info').innerHTML = infoHtml;

        // 加载资源数据
        await loadResourceData();
    } catch (e) {
        console.error(e);
        alert('加载服务器信息失败：' + e.message);
    }
}

async function loadResourceData() {
    try {
        // 获取最新资源数据
        const latestResource = await fetchJSON(`/api/servers/${serverId}/resources/latest`);

        if (latestResource) {
            // 更新实时指标
            document.getElementById('cpu-value').textContent = formatDecimal(latestResource.cpuUsage) + '%';
            document.getElementById('memory-value').textContent = formatDecimal(latestResource.memoryUsage) + '%';
            document.getElementById('disk-value').textContent = formatDecimal(latestResource.diskUsage) + '%';
            document.getElementById('load-value').textContent = formatDecimal(latestResource.loadAverage);

            // 更新饼图
            updatePieChart('cpu-pie-chart', latestResource.cpuUsage, '#5470c6');
            updatePieChart('memory-pie-chart', latestResource.memoryUsage, '#91cc75');
            updatePieChart('disk-pie-chart', latestResource.diskUsage, '#fac858');
            updateLoadGauge(latestResource.loadAverage);
        }

        // 获取历史数据（最近100条，这样可以保存更多历史记录）
        const historyResources = await fetchJSON(`/api/servers/${serverId}/resources?limit=100`);

        if (historyResources && historyResources.length > 0) {
            // 按时间排序
            const sortedData = historyResources.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

            // 更新趋势图
            updateLineChart(sortedData, {
                elementId: 'trend-chart',
                series: [
                    { key: 'cpuUsage', name: 'CPU使用率', color: '#5470c6' },
                    { key: 'memoryUsage', name: '内存使用率', color: '#91cc75' },
                    { key: 'diskUsage', name: '磁盘使用率', color: '#fac858' }
                ]
            });

            // 更新网络流量图
            updateBarChart(sortedData, {
                elementId: 'network-chart',
                series: [
                    { key: 'networkIn', name: '网络入流量', color: '#73c0de' },
                    { key: 'networkOut', name: '网络出流量', color: '#3ba272' }
                ]
            });

            // 更新数据表格
            updateHistoryTable(sortedData);
        } else {
            // 如果没有历史数据，图表函数会自动显示"暂无数据"提示
            updateLineChart([], {
                elementId: 'trend-chart',
                series: []
            });
            updateBarChart([], {
                elementId: 'network-chart',
                series: []
            });
            document.getElementById('history-table-body').innerHTML = '<tr><td colspan="7" style="text-align: center; color: #999;">暂无历史数据</td></tr>';
        }
    } catch (e) {
        console.error(e);
        alert('加载资源数据失败：' + e.message);
    }
}

function updateHistoryTable(data) {
    const tbody = document.getElementById('history-table-body');
    tbody.innerHTML = '';

    // 倒序显示，最新的数据在上面
    const reversedData = [...data].reverse();

    reversedData.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${formatDateTime(item.timestamp)}</td>
            <td>${formatDecimal(item.cpuUsage)}%</td>
            <td>${formatDecimal(item.memoryUsage)}%</td>
            <td>${formatDecimal(item.diskUsage)}%</td>
            <td>${formatDecimal(item.loadAverage)}</td>
            <td>${formatDecimal(item.networkIn)}</td>
            <td>${formatDecimal(item.networkOut)}</td>
        `;
        tbody.appendChild(row);
    });
}

async function refreshDetail() {
    try {
        // 先采集最新数据
        await fetchJSON(`/api/servers/${serverId}/resources/collect`, {
            method: 'POST',
            body: JSON.stringify({ count: 1 })
        });

        // 重新加载数据
        await loadResourceData();
    } catch (e) {
        console.error(e);
        alert('刷新失败：' + e.message);
    }
}

function toggleAutoRefresh() {
    const checkbox = document.getElementById('auto-refresh');
    if (checkbox.checked) {
        // 开启自动刷新
        if (autoRefreshInterval) {
            clearInterval(autoRefreshInterval);
        }
        autoRefreshInterval = setInterval(() => {
            refreshDetail();
        }, 30000); // 30秒刷新一次
        console.log('自动刷新已开启，每30秒刷新一次');
    } else {
        // 关闭自动刷新
        if (autoRefreshInterval) {
            clearInterval(autoRefreshInterval);
            autoRefreshInterval = null;
            console.log('自动刷新已关闭');
        }
    }
}

// 页面卸载时清除自动刷新定时器
window.addEventListener('beforeunload', () => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});

// 响应窗口大小变化，重新绘制图表
window.addEventListener('resize', () => {
    loadResourceData().catch(e => console.error(e));
});

// 页面加载完成后加载数据
window.addEventListener('DOMContentLoaded', () => {
    if (!serverId) {
        alert('未指定服务器ID');
        goBack();
        return;
    }

    loadServerInfo().catch(e => console.error(e));
});
