const state = {
    accessToken: sessionStorage.getItem('secureAgentAccessToken') || null
};

const connectionPill = document.getElementById('connectionPill');
const connectionText = document.getElementById('connectionText');
const executionCount = document.getElementById('executionCount');
const approvalCount = document.getElementById('approvalCount');
const executionList = document.getElementById('executionList');
const approvalList = document.getElementById('approvalList');
const authMessage = document.getElementById('authMessage');

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) {
        headers.set('Authorization', `Bearer ${state.accessToken}`);
    }
    if (options.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(path, { ...options, headers });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `${response.status} ${response.statusText}`);
    }

    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response.text();
}

async function checkHealth() {
    try {
        await api('/actuator/health');
        connectionPill.classList.remove('offline');
        connectionPill.classList.add('online');
        connectionText.textContent = 'API online';
    } catch (_) {
        connectionPill.classList.remove('online');
        connectionPill.classList.add('offline');
        connectionText.textContent = 'API offline';
    }
}

async function login(username, password) {
    const data = await api('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
    });

    const token = data.accessToken || data.access_token || data.token;
    if (!token) {
        throw new Error('Login succeeded but no access token was returned.');
    }

    state.accessToken = token;
    sessionStorage.setItem('secureAgentAccessToken', token);
    authMessage.textContent = `Authenticated as ${username}.`;
    await loadDashboardData();
}

async function loadDashboardData() {
    if (!state.accessToken) {
        executionCount.textContent = '—';
        approvalCount.textContent = '—';
        executionList.innerHTML = '<div class="empty-state">Authenticate to load execution data.</div>';
        approvalList.innerHTML = '<div class="empty-state">Authenticate to load approval data.</div>';
        return;
    }

    const results = await Promise.allSettled([
        api('/api/v1/agents/executions'),
        api('/api/v1/approvals/pending'),
        api('/api/v1/users/me')
    ]);

    const executionsResult = results[0];
    const approvalsResult = results[1];
    const profileResult = results[2];

    if (executionsResult.status === 'fulfilled') {
        const executions = Array.isArray(executionsResult.value) ? executionsResult.value : [];
        executionCount.textContent = executions.length;
        renderExecutions(executions);
    } else {
        executionCount.textContent = '—';
        executionList.innerHTML = '<div class="empty-state">Unable to load executions for this session.</div>';
    }

    if (approvalsResult.status === 'fulfilled') {
        const approvals = Array.isArray(approvalsResult.value) ? approvalsResult.value : [];
        approvalCount.textContent = approvals.length;
        renderApprovals(approvals);
    } else {
        approvalCount.textContent = '—';
        approvalList.innerHTML = '<div class="empty-state">Current role cannot access the approval queue.</div>';
    }

    if (profileResult.status === 'fulfilled') {
        const profile = profileResult.value || {};
        authMessage.textContent = `Connected as ${profile.username || 'authenticated user'}.`;
    }
}

function renderExecutions(executions) {
    if (!executions.length) {
        executionList.innerHTML = '<div class="empty-state">No executions found.</div>';
        return;
    }

    executionList.innerHTML = executions.slice(0, 6).map(item => {
        const id = item.id || item.executionId || 'unknown';
        const agent = item.agent || item.agentName || 'agent';
        const status = item.status || item.state || 'UNKNOWN';
        return `
            <div class="activity-item">
                <div class="activity-main">
                    <strong>${escapeHtml(agent)}</strong>
                    <div class="activity-meta">${escapeHtml(id)}</div>
                </div>
                <span class="badge">${escapeHtml(status)}</span>
            </div>`;
    }).join('');
}

function renderApprovals(approvals) {
    if (!approvals.length) {
        approvalList.innerHTML = '<div class="empty-state">No pending approvals.</div>';
        return;
    }

    approvalList.innerHTML = approvals.slice(0, 5).map(item => {
        const tool = item.tool || item.toolName || 'critical tool';
        const id = item.id || item.approvalId || 'pending';
        return `
            <div class="approval-item">
                <div>
                    <strong>${escapeHtml(tool)}</strong>
                    <div class="activity-meta">${escapeHtml(id)}</div>
                </div>
                <span class="badge next">Pending</span>
            </div>`;
    }).join('');
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

document.getElementById('loginForm').addEventListener('submit', async event => {
    event.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    authMessage.textContent = 'Authenticating...';
    try {
        await login(username, password);
    } catch (error) {
        authMessage.textContent = `Authentication failed: ${error.message}`;
    }
});

document.getElementById('logoutButton').addEventListener('click', () => {
    state.accessToken = null;
    sessionStorage.removeItem('secureAgentAccessToken');
    authMessage.textContent = 'Session cleared.';
    loadDashboardData();
});

document.getElementById('refreshButton').addEventListener('click', async () => {
    await checkHealth();
    await loadDashboardData();
});

checkHealth();
loadDashboardData();
