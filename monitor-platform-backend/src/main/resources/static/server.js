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

function getResourceClass(percentage) {
    if (percentage >= 80) return "high";
    if (percentage >= 60) return "medium";
    return "low";
}

async function loadServers() {
    try {
        const servers = await fetchJSON("/api/servers");
        const serverList = document.getElementById("server-list");
        const emptyState = document.getElementById("empty-state");

        if (!servers || servers.length === 0) {
            serverList.innerHTML = "";
            emptyState.style.display = "block";
            return;
        }

        emptyState.style.display = "none";
        serverList.innerHTML = "";

        for (const server of servers) {
            // 获取最新资源数据
            const resources = await fetchJSON(`/api/servers/${server.id}/resources/latest`);
            const resource = resources;

            const card = document.createElement("div");
            card.className = "server-card";
            card.style.cursor = "pointer";
            card.onclick = function() {
                window.location.href = `server-detail.html?id=${server.id}`;
            };

            const statusClass = server.status || "offline";

            card.innerHTML = `
                <h3>
                    ${server.name}
                    <span class="status-badge ${statusClass}">${server.status === "online" ? "在线" : "离线"}</span>
                </h3>
                <div class="server-info">
                    <div class="label">IP地址: <span class="value">${server.ip || "-"}</span></div>
                    <div class="label">类型: <span class="value">${server.type || "-"}</span></div>
                    <div class="label">环境: <span class="value">${server.env || "-"}</span></div>
                </div>
                ${resource ? `
                <div class="resource-bar">
                    <div class="resource-bar-label">
                        <span>CPU</span>
                        <span>${formatDecimal(resource.cpuUsage)}%</span>
                    </div>
                    <div class="resource-bar-track">
                        <div class="resource-bar-fill ${getResourceClass(resource.cpuUsage)}" style="width: ${resource.cpuUsage}%"></div>
                    </div>
                </div>
                <div class="resource-bar">
                    <div class="resource-bar-label">
                        <span>内存</span>
                        <span>${formatDecimal(resource.memoryUsage)}% (${formatDecimal(resource.memoryUsed)}/${formatDecimal(server.totalMemory)} GB)</span>
                    </div>
                    <div class="resource-bar-track">
                        <div class="resource-bar-fill ${getResourceClass(resource.memoryUsage)}" style="width: ${resource.memoryUsage}%"></div>
                    </div>
                </div>
                <div class="resource-bar">
                    <div class="resource-bar-label">
                        <span>磁盘</span>
                        <span>${formatDecimal(resource.diskUsage)}% (${formatDecimal(resource.diskUsed)}/${formatDecimal(server.totalDisk)} GB)</span>
                    </div>
                    <div class="resource-bar-track">
                        <div class="resource-bar-fill ${getResourceClass(resource.diskUsage)}" style="width: ${resource.diskUsage}%"></div>
                    </div>
                </div>
                <div class="server-info" style="margin-top: 10px;">
                    <div class="label">负载: <span class="value">${formatDecimal(resource.loadAverage)}</span></div>
                    <div class="label">网络入: <span class="value">${formatDecimal(resource.networkIn)} MB/s</span></div>
                    <div class="label">网络出: <span class="value">${formatDecimal(resource.networkOut)} MB/s</span></div>
                </div>
                ` : '<p style="color: #999; font-size: 13px;">暂无资源数据</p>'}
                <div class="server-actions">
                    <button onclick="event.stopPropagation(); collectResources(${server.id})">采集资源</button>
                    <button onclick="event.stopPropagation(); refreshServer(${server.id})">刷新</button>
                    <button onclick="event.stopPropagation(); deleteServer(${server.id})" style="background: #f5222d;">删除</button>
                    <button onclick="event.stopPropagation(); window.location.href='server-detail.html?id=${server.id}'" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">详情</button>
                </div>
            `;

            serverList.appendChild(card);
        }
    } catch (e) {
        console.error(e);
        alert("加载服务器列表失败：" + e.message);
    }
}

function showAddServerModal() {
    document.getElementById("add-server-modal").classList.add("active");
}

function hideAddServerModal() {
    document.getElementById("add-server-modal").classList.remove("active");
    document.getElementById("add-server-form").reset();
}

async function handleAddServer(event) {
    event.preventDefault();

    const serverInfo = {
        name: document.getElementById("server-name").value.trim(),
        ip: document.getElementById("server-ip").value.trim(),
        type: document.getElementById("server-type").value,
        env: document.getElementById("server-env").value.trim(),
        cpuCores: parseInt(document.getElementById("server-cpu").value) || null,
        totalMemory: parseFloat(document.getElementById("server-memory").value) || null,
        totalDisk: parseFloat(document.getElementById("server-disk").value) || null,
        description: document.getElementById("server-desc").value.trim(),
        status: "online"
    };

    try {
        await fetchJSON("/api/servers", {
            method: "POST",
            body: JSON.stringify(serverInfo)
        });
        hideAddServerModal();
        await loadServers();
    } catch (e) {
        console.error(e);
        alert("添加服务器失败：" + e.message);
    }
}

async function collectResources(serverId) {
    try {
        await fetchJSON(`/api/servers/${serverId}/resources/collect`, {
            method: "POST",
            body: JSON.stringify({ count: 30 })
        });
        await loadServers();
    } catch (e) {
        console.error(e);
        alert("采集资源失败：" + e.message);
    }
}

async function refreshServer(serverId) {
    await loadServers();
}

async function deleteServer(serverId) {
    if (!confirm("确定要删除这台服务器吗？")) {
        return;
    }
    try {
        await fetchJSON(`/api/servers/${serverId}`, {
            method: "DELETE"
        });
        await loadServers();
    } catch (e) {
        console.error(e);
        alert("删除服务器失败：" + e.message);
    }
}

window.addEventListener("DOMContentLoaded", () => {
    // 确保DOM元素存在后再添加事件监听
    const addServerForm = document.getElementById("add-server-form");
    if (addServerForm) {
        addServerForm.addEventListener("submit", handleAddServer);
    }

    loadServers().catch(e => console.error(e));
});
