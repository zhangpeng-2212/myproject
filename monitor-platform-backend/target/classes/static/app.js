let currentServiceId = null;
let servicesCache = [];

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

function formatTime(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleString();
}

function renderServices() {
    const listEl = document.getElementById("service-list");
    listEl.innerHTML = "";
    servicesCache.forEach(svc => {
        const li = document.createElement("li");
        li.dataset.id = svc.id;
        li.className = svc.id === currentServiceId ? "active" : "";
        li.innerHTML = `
            <span>${svc.name}</span>
            <span class="env">${svc.env || ""}</span>
        `;
        li.addEventListener("click", () => {
            selectService(svc.id);
        });
        listEl.appendChild(li);
    });
}

function updateServiceDetail() {
    const emptyEl = document.getElementById("service-detail-empty");
    const detailEl = document.getElementById("service-detail");

    if (!currentServiceId) {
        emptyEl.classList.remove("hidden");
        detailEl.classList.add("hidden");
        return;
    }

    const svc = servicesCache.find(s => s.id === currentServiceId);
    if (!svc) {
        emptyEl.classList.remove("hidden");
        detailEl.classList.add("hidden");
        return;
    }

    emptyEl.classList.add("hidden");
    detailEl.classList.remove("hidden");

    document.getElementById("detail-name").textContent = svc.name;
    document.getElementById("detail-env").textContent = svc.env || "-";
    document.getElementById("detail-desc").textContent = svc.description || "-";
}

async function loadServices() {
    try {
        const data = await fetchJSON("/api/services");
        servicesCache = data || [];
        if (!currentServiceId && servicesCache.length > 0) {
            currentServiceId = servicesCache[0].id;
        }
        renderServices();
        updateServiceDetail();
        await refreshAllData();
    } catch (e) {
        console.error(e);
        alert("加载服务列表失败：" + e.message);
    }
}

async function selectService(id) {
    currentServiceId = id;
    renderServices();
    updateServiceDetail();
    await refreshAllData();
}

async function refreshAllData() {
    if (!currentServiceId) {
        document.getElementById("metrics-body").innerHTML = "";
        document.getElementById("anomalies-body").innerHTML = "";
        return;
    }
    await Promise.all([loadMetrics(), loadAnomalies()]);
}

async function loadMetrics() {
    const tbody = document.getElementById("metrics-body");
    tbody.innerHTML = "";
    if (!currentServiceId) return;
    try {
        const data = await fetchJSON(`/api/metrics/${currentServiceId}?limit=50`);
        (data || []).forEach(item => {
            const tr = document.createElement("tr");
            const tdTime = document.createElement("td");
            const tdValue = document.createElement("td");
            tdTime.textContent = formatTime(item.timestamp);
            tdValue.textContent = item.value != null ? item.value.toFixed(2) : "";
            tr.appendChild(tdTime);
            tr.appendChild(tdValue);
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
        alert("加载指标失败：" + e.message);
    }
}

async function loadAnomalies() {
    const tbody = document.getElementById("anomalies-body");
    tbody.innerHTML = "";
    if (!currentServiceId) return;
    try {
        const data = await fetchJSON(`/api/anomalies?serviceId=${currentServiceId}&limit=50`);
        (data || []).forEach(item => {
            const tr = document.createElement("tr");

            const tdTime = document.createElement("td");
            tdTime.textContent = formatTime(item.createdAt || item.startTime);

            const tdSeverity = document.createElement("td");
            const badge = document.createElement("span");
            badge.classList.add("badge");
            const sev = (item.severity || "").toLowerCase();
            if (sev === "high") {
                badge.classList.add("badge-high");
                badge.textContent = "高";
            } else if (sev === "medium") {
                badge.classList.add("badge-medium");
                badge.textContent = "中";
            } else {
                badge.classList.add("badge-low");
                badge.textContent = "低";
            }
            tdSeverity.appendChild(badge);

            const tdScore = document.createElement("td");
            tdScore.textContent = item.score != null ? item.score.toFixed(2) : "";

            const tdReason = document.createElement("td");
            tdReason.textContent = item.reason || "";

            tr.appendChild(tdTime);
            tr.appendChild(tdSeverity);
            tr.appendChild(tdScore);
            tr.appendChild(tdReason);

            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
        alert("加载异常事件失败：" + e.message);
    }
}

async function handleAddService(event) {
    event.preventDefault();
    const name = document.getElementById("service-name").value.trim();
    if (!name) {
        alert("服务名称不能为空");
        return;
    }
    const env = document.getElementById("service-env").value.trim();
    const desc = document.getElementById("service-desc").value.trim();
    try {
        const created = await fetchJSON("/api/services", {
            method: "POST",
            body: JSON.stringify({
                name: name,
                env: env,
                description: desc
            })
        });
        document.getElementById("service-name").value = "";
        document.getElementById("service-env").value = "";
        document.getElementById("service-desc").value = "";
        servicesCache.push(created);
        currentServiceId = created.id;
        renderServices();
        updateServiceDetail();
        await refreshAllData();
    } catch (e) {
        console.error(e);
        alert("创建服务失败：" + e.message);
    }
}

async function handleGenerateMetrics() {
    if (!currentServiceId) {
        alert("请先选择一个服务");
        return;
    }
    try {
        await fetchJSON("/api/metrics/collect", {
            method: "POST",
            body: JSON.stringify({
                serviceId: currentServiceId,
                count: 30
            })
        });
        await loadMetrics();
    } catch (e) {
        console.error(e);
        alert("生成模拟指标失败：" + e.message);
    }
}

async function handleDetectAnomaly() {
    if (!currentServiceId) {
        alert("请先选择一个服务");
        return;
    }
    try {
        await fetchJSON("/api/anomalies/detect", {
            method: "POST",
            body: JSON.stringify({
                serviceId: currentServiceId
            })
        });
        await loadAnomalies();
    } catch (e) {
        console.error(e);
        alert("触发异常检测失败：" + e.message);
    }
}

function initEvents() {
    document.getElementById("add-service-form")
        .addEventListener("submit", handleAddService);
    document.getElementById("btn-generate-metrics")
        .addEventListener("click", handleGenerateMetrics);
    document.getElementById("btn-detect-anomaly")
        .addEventListener("click", handleDetectAnomaly);
    document.getElementById("btn-refresh")
        .addEventListener("click", () => {
            refreshAllData().catch(e => console.error(e));
        });
}

window.addEventListener("DOMContentLoaded", () => {
    initEvents();
    loadServices().catch(e => console.error(e));
});

