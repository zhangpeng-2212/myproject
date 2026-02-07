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

function addMessage(role, content) {
    const chatMessages = document.getElementById("chat-messages");
    const messageDiv = document.createElement("div");
    messageDiv.className = `message ${role}`;

    const contentDiv = document.createElement("div");
    contentDiv.className = "message-content";
    contentDiv.textContent = content;

    messageDiv.appendChild(contentDiv);
    chatMessages.appendChild(messageDiv);

    // 滚动到底部
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function showLoading() {
    const chatMessages = document.getElementById("chat-messages");
    const messageDiv = document.createElement("div");
    messageDiv.className = "message system";
    messageDiv.id = "loading-message";

    const contentDiv = document.createElement("div");
    contentDiv.className = "message-content";
    contentDiv.innerHTML = '<span class="loading"></span>';

    messageDiv.appendChild(contentDiv);
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function hideLoading() {
    const loadingMessage = document.getElementById("loading-message");
    if (loadingMessage) {
        loadingMessage.remove();
    }
}

async function sendMessage() {
    const input = document.getElementById("chat-input");
    const sendButton = document.getElementById("send-button");
    const query = input.value.trim();

    if (!query) {
        return;
    }

    // 添加用户消息
    addMessage("user", query);

    // 清空输入框并禁用按钮
    input.value = "";
    sendButton.disabled = true;

    // 显示加载中
    showLoading();

    try {
        const response = await fetchJSON("/api/chat/query", {
            method: "POST",
            body: JSON.stringify({ query })
        });

        hideLoading();

        if (response && response.response) {
            addMessage("system", response.response);
        } else {
            addMessage("system", "抱歉，未能获取到回答，请稍后重试。");
        }
    } catch (e) {
        hideLoading();
        console.error(e);
        addMessage("system", "查询失败：" + e.message);
    } finally {
        sendButton.disabled = false;
        input.focus();
    }
}

function handleKeyPress(event) {
    if (event.key === "Enter") {
        sendMessage();
    }
}

function askSuggestion(question) {
    const input = document.getElementById("chat-input");
    input.value = question;
    sendMessage();
}

// 自动聚焦输入框
window.addEventListener("DOMContentLoaded", () => {
    document.getElementById("chat-input").focus();
});
