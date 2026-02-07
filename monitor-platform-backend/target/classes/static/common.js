// 工具函数：节流
function throttle(func, delay) {
    let lastCall = 0;
    return function(...args) {
        const now = Date.now();
        if (now - lastCall >= delay) {
            lastCall = now;
            return func.apply(this, args);
        }
    };
}

// 工具函数：防抖
function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    };
}

// 工具函数：使用文档片段优化 DOM 插入
function createElementFromString(html) {
    const template = document.createElement('template');
    template.innerHTML = html.trim();
    return template.content.firstChild;
}

// 工具函数：批量更新 DOM
function batchDOMUpdates(updates) {
    requestAnimationFrame(() => {
        updates.forEach(update => update());
    });
}

// 优化的 fetchJSON，使用 AbortController
async function fetchJSON(url, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 30000); // 30秒超时

    try {
        const resp = await fetch(url, {
            headers: {
                "Content-Type": "application/json"
            },
            signal: controller.signal,
            ...options
        });
        clearTimeout(timeoutId);

        if (!resp.ok) {
            const text = await resp.text();
            throw new Error(`请求失败: ${resp.status} ${text}`);
        }
        const contentType = resp.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return resp.json();
        }
        return null;
    } catch (error) {
        clearTimeout(timeoutId);
        if (error.name === 'AbortError') {
            throw new Error('请求超时');
        }
        throw error;
    }
}

// 优化的日期格式化
function formatTime(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    const now = new Date();
    const diff = now - d;

    // 如果是最近24小时，显示相对时间
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;

    return d.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 优化的数字格式化
function formatNumber(num, digits = 2) {
    if (num == null) return "-";
    if (num >= 10000) {
        return (num / 1000).toFixed(digits) + 'k';
    }
    return num.toLocaleString('zh-CN', {
        maximumFractionDigits: digits
    });
}

// 导出工具函数
window.monitorUtils = {
    throttle,
    debounce,
    createElementFromString,
    batchDOMUpdates,
    fetchJSON,
    formatTime,
    formatNumber
};

// 弹窗点击遮罩层关闭功能
function initModalOverlayClick() {
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', function(e) {
            // 只有点击遮罩层本身时才关闭，点击弹窗内容区域不关闭
            if (e.target === this) {
                this.classList.remove('active');
            }
        });
    });
}

// 键盘ESC关闭弹窗
function initModalEscapeKey() {
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay.active').forEach(overlay => {
                overlay.classList.remove('active');
            });
        }
    });
}

// 页面加载后初始化弹窗功能
window.addEventListener('DOMContentLoaded', function() {
    initModalOverlayClick();
    initModalEscapeKey();
});
