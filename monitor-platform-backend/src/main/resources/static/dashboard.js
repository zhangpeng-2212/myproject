// 使用通用工具函数
const { fetchJSON, formatTime, formatNumber } = window.monitorUtils || {};

function formatDecimal(num, digits = 2) {
    if (num == null) return "-";
    return num.toFixed(digits);
}

function getSeverityClass(severity) {
    const sev = (severity || "").toLowerCase();
    if (sev === "high") return "badge-high";
    if (sev === "medium") return "badge-medium";
    return "badge-low";
}

function getSeverityLabel(severity) {
    const sev = (severity || "").toLowerCase();
    if (sev === "high") return "高";
    if (sev === "medium") return "中";
    return "低";
}

async function loadDashboardData() {
    try {
        const summary = await fetchJSON("/api/dashboard/summary");

        // 基础统计
        document.getElementById("total-services").textContent = formatNumber(summary.totalServices);
        document.getElementById("online-services").textContent = formatNumber(summary.onlineServices);
        document.getElementById("total-servers").textContent = formatNumber(summary.totalServers);
        document.getElementById("online-servers").textContent = formatNumber(summary.onlineServers);
        document.getElementById("recent-anomalies").textContent = formatNumber(summary.recentAnomalies);
        document.getElementById("today-anomalies").textContent = formatNumber(summary.todayAnomalies);

        // 服务器资源
        document.getElementById("total-cpu-cores").textContent = formatNumber(summary.totalCpuCores);
        document.getElementById("total-memory").textContent = formatDecimal(summary.totalMemory, 2);
        document.getElementById("total-disk").textContent = formatDecimal(summary.totalDisk, 2);

        // 异常统计
        const highSeverityEl = document.getElementById("high-severity");
        highSeverityEl.textContent = formatNumber(summary.highSeverityAnomalies);
        if (summary.highSeverityAnomalies > 0) {
            highSeverityEl.classList.add("error");
        }

        document.getElementById("medium-severity").textContent = formatNumber(summary.mediumSeverityAnomalies);
        document.getElementById("low-severity").textContent = formatNumber(summary.lowSeverityAnomalies);

        // 按环境统计服务
        const servicesByEnvList = document.getElementById("services-by-env");
        servicesByEnvList.innerHTML = "";
        if (summary.servicesByEnv) {
            Object.entries(summary.servicesByEnv).forEach(([env, count]) => {
                const li = document.createElement("li");
                li.innerHTML = `
                    <span class="label">${env}</span>
                    <span class="value">${count}</span>
                `;
                servicesByEnvList.appendChild(li);
            });
        }

        // 按类型统计服务器
        const serversByTypeList = document.getElementById("servers-by-type");
        serversByTypeList.innerHTML = "";
        if (summary.serversByType) {
            Object.entries(summary.serversByType).forEach(([type, count]) => {
                const li = document.createElement("li");
                li.innerHTML = `
                    <span class="label">${type}</span>
                    <span class="value">${count}</span>
                `;
                serversByTypeList.appendChild(li);
            });
        }

        // 最近异常事件
        const anomaliesList = document.getElementById("recent-anomalies-list");
        anomaliesList.innerHTML = "";
        if (summary.recentAnomalyEvents && summary.recentAnomalyEvents.length > 0) {
            summary.recentAnomalyEvents.forEach(item => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${formatTime(item.createdAt || item.startTime)}</td>
                    <td><span class="badge ${getSeverityClass(item.severity)}">${getSeverityLabel(item.severity)}</span></td>
                    <td>${item.serviceId}</td>
                    <td>${formatDecimal(item.score)}</td>
                    <td>${item.reason || ""}</td>
                `;
                anomaliesList.appendChild(tr);
            });
        } else {
            anomaliesList.innerHTML = "<tr><td colspan='5' style='text-align:center;'>暂无数据</td></tr>";
        }

        // 服务指标趋势
        const trendsList = document.getElementById("metric-trends-list");
        trendsList.innerHTML = "";
        if (summary.metricTrends && summary.metricTrends.length > 0) {
            summary.metricTrends.forEach(trend => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${trend.serviceName}</td>
                    <td>${formatDecimal(trend.avgResponseTime)}</td>
                    <td>${formatDecimal(trend.maxResponseTime)}</td>
                    <td>${formatDecimal(trend.minResponseTime)}</td>
                    <td>${formatTime(trend.lastUpdated)}</td>
                `;
                trendsList.appendChild(tr);
            });
        } else {
            trendsList.innerHTML = "<tr><td colspan='5' style='text-align:center;'>暂无数据</td></tr>";
        }
    } catch (e) {
        console.error(e);
        alert("加载大屏数据失败：" + e.message);
    }
}

// 自动刷新 - 使用节流优化
let isRefreshing = false;
const throttledRefresh = throttle(() => {
    if (!isRefreshing) {
        isRefreshing = true;
        loadDashboardData()
            .finally(() => {
                isRefreshing = false;
            })
            .catch(e => console.error(e));
    }
}, 30000);

setInterval(throttledRefresh, 30000);

// 初始加载
window.addEventListener("DOMContentLoaded", () => {
    loadDashboardData().catch(e => console.error(e));
});
