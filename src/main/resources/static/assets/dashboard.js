const state = {
    accessToken: sessionStorage.getItem('secureAgentAccessToken') || null,
    refreshToken: sessionStorage.getItem('secureAgentRefreshToken') || null,
    profile: null,
    canApprove: false
};

const connectionPill = document.getElementById('connectionPill');
const connectionText = document.getElementById('connectionText');
const executionCount = document.getElementById('executionCount');
const approvalCount = document.getElementById('approvalCount');
const executionList = document.getElementById('executionList');
const approvalList = document.getElementById('approvalList');
const authMessage = document.getElementById('authMessage');
const userIdentity = document.getElementById('userIdentity');
const currentUsername = document.getElementById('currentUsername');
const currentRoles = document.getElementById('currentRoles');
const loginForm = document.getElementById('loginForm');
const sessionPanel = document.getElementById('sessionPanel');
const sessionUsername = document.getElementById('sessionUsername');
const sessionRoles = document.getElementById('sessionRoles');
const agentPromptForm = document.getElementById('agentPromptForm');
const agentPrompt = document.getElementById('agentPrompt');
const agentMessage = document.getElementById('agentMessage');
const agentSubmitButton = document.getElementById('agentSubmitButton');

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken && options.auth !== false) {
        headers.set('Authorization', `Bearer ${state.accessToken}`);
    }
    if (options.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const { auth, ...fetchOptions } = options;
    const response = await fetch(path, { ...fetchOptions, headers });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `${response.status} ${response.statusText}`);
    }

    if (response.status === 204 || response.headers.get('content-length') === '0') return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response.text();
}

async function checkHealth() {
    try {
        await api('/actuator/health', { auth: false });
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
        auth: false,
        body: JSON.stringify({ username, password })
    });

    if (!data?.accessToken) {
        throw new Error('Login succeeded but no access token was returned.');
    }

    state.accessToken = data.accessToken;
    state.refreshToken = data.refreshToken || null;
    sessionStorage.setItem('secureAgentAccessToken', state.accessToken);
    if (state.refreshToken) sessionStorage.setItem('secureAgentRefreshToken', state.refreshToken);

    authMessage.textContent = `Authenticated as ${username}.`;
    await loadDashboardData();
}

async function logout() {
    if (state.refreshToken) {
        try {
            await api('/api/v1/auth/logout', {
                method: 'POST',
                auth: false,
                body: JSON.stringify({ refreshToken: state.refreshToken })
            });
        } catch (_) {
            // Local session is cleared even if token revocation cannot be reached.
        }
    }
    clearSession();
    renderSignedOut();
}

function clearSession() {
    state.accessToken = null;
    state.refreshToken = null;
    state.profile = null;
    state.canApprove = false;
    sessionStorage.removeItem('secureAgentAccessToken');
    sessionStorage.removeItem('secureAgentRefreshToken');
}

function normalizedRoles(authorities = []) {
    return authorities.map(role => String(role).replace(/^ROLE_/, ''));
}

function renderProfile(profile) {
    state.profile = profile || {};
    const authorities = Array.isArray(state.profile.authorities) ? state.profile.authorities : [];
    const roles = normalizedRoles(authorities);
    state.canApprove = authorities.includes('ROLE_OPERATOR') || authorities.includes('ROLE_ADMIN');

    const username = state.profile.username || 'authenticated user';
    const roleText = roles.length ? roles.join(' · ') : 'AUTHENTICATED';

    currentUsername.textContent = username;
    currentRoles.textContent = roleText;
    sessionUsername.textContent = username;
    sessionRoles.textContent = roleText;
    userIdentity.hidden = false;
    loginForm.hidden = true;
    sessionPanel.hidden = false;
    authMessage.textContent = `Connected as ${username}.`;
    agentMessage.textContent = 'Submit an intent. Policy Engine remains authoritative.';
}

function renderSignedOut() {
    executionCount.textContent = '—';
    approvalCount.textContent = '—';
    executionList.innerHTML = '<div class="empty-state">Authenticate to load execution data.</div>';
    approvalList.innerHTML = '<div class="empty-state">Authenticate to load approval data.</div>';
    userIdentity.hidden = true;
    loginForm.hidden = false;
    sessionPanel.hidden = true;
    authMessage.textContent = 'Session cleared.';
    agentMessage.textContent = 'Authenticate before submitting an agent execution.';
}

async function loadDashboardData() {
    if (!state.accessToken) {
        renderSignedOut();
        return;
    }

    const [executionsResult, profileResult] = await Promise.allSettled([
        api('/api/v1/agents/executions'),
        api('/api/v1/users/me')
    ]);

    if (profileResult.status === 'fulfilled') {
        renderProfile(profileResult.value);
    } else {
        clearSession();
        renderSignedOut();
        authMessage.textContent = 'Session expired. Authenticate again.';
        return;
    }

    if (executionsResult.status === 'fulfilled') {
        const executions = Array.isArray(executionsResult.value) ? executionsResult.value : [];
        executionCount.textContent = executions.length;
        renderExecutions(executions);
    } else {
        executionCount.textContent = '—';
        executionList.innerHTML = '<div class="empty-state">Unable to load executions for this session.</div>';
    }

    await loadApprovals();
}

async function loadApprovals() {
    if (!state.canApprove) {
        approvalCount.textContent = '—';
        approvalList.innerHTML = '<div class="empty-state">Operator or Admin role required to review approvals.</div>';
        return;
    }

    try {
        const approvals = await api('/api/v1/approvals/pending');
        const list = Array.isArray(approvals) ? approvals : [];
        approvalCount.textContent = list.length;
        renderApprovals(list);
    } catch (error) {
        approvalCount.textContent = '—';
        approvalList.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
    }
}

async function createExecution(agent, prompt) {
    return api('/api/v1/agents/executions', {
        method: 'POST',
        body: JSON.stringify({ agent, prompt })
    });
}

async function decideApproval(id, action) {
    await api(`/api/v1/approvals/${encodeURIComponent(id)}/${action}`, { method: 'POST' });
    await loadDashboardData();
}

function renderExecutions(executions) {
    if (!executions.length) {
        executionList.innerHTML = '<div class="empty-state">No executions found.</div>';
        return;
    }

    executionList.innerHTML = executions.slice(0, 5).map(item => {
        const tool = item.requestedTool ? ` · ${item.requestedTool}` : '';
        const detail = item.result || item.prompt || '';
        return `
            <div class="activity-item">
                <div class="activity-main">
                    <strong>${escapeHtml(item.agent || 'agent')}</strong>
                    <div class="activity-meta">${escapeHtml(item.id || 'unknown')}${escapeHtml(tool)}</div>
                    ${detail ? `<div class="activity-detail">${escapeHtml(detail)}</div>` : ''}
                </div>
                <span class="badge ${statusClass(item.status)}">${escapeHtml(item.status || 'UNKNOWN')}</span>
            </div>`;
    }).join('');
}

function renderApprovals(approvals) {
    if (!approvals.length) {
        approvalList.innerHTML = '<div class="empty-state">No pending approvals.</div>';
        return;
    }

    approvalList.innerHTML = approvals.slice(0, 5).map(item => `
        <div class="approval-item approval-card">
            <div>
                <strong>${escapeHtml(item.toolName || 'critical tool')}</strong>
                <div class="activity-meta">${escapeHtml(item.reason || item.executionId || item.id)}</div>
            </div>
            <div class="approval-actions">
                <button class="approval-button approve" data-action="approve" data-id="${escapeHtml(item.id)}" type="button">Approve</button>
                <button class="approval-button reject" data-action="reject" data-id="${escapeHtml(item.id)}" type="button">Reject</button>
            </div>
        </div>`).join('');
}

function statusClass(status) {
    const value = String(status || '').toUpperCase();
    if (value.includes('COMPLETED') || value.includes('APPROVED')) return 'live';
    if (value.includes('PENDING') || value.includes('APPROVAL')) return 'next';
    if (value.includes('DENIED') || value.includes('REJECTED') || value.includes('FAILED')) return 'danger';
    return '';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

loginForm.addEventListener('submit', async event => {
    event.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    authMessage.textContent = 'Authenticating...';
    try {
        await login(username, password);
        document.getElementById('password').value = '';
    } catch (error) {
        authMessage.textContent = `Authentication failed: ${error.message}`;
    }
});

agentPromptForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (!state.accessToken) {
        agentMessage.textContent = 'Authenticate before submitting an execution.';
        return;
    }

    const agent = document.getElementById('agentName').value;
    const prompt = agentPrompt.value.trim();
    if (!prompt) return;

    agentSubmitButton.disabled = true;
    agentMessage.textContent = 'Submitting governed execution...';
    try {
        const execution = await createExecution(agent, prompt);
        agentMessage.textContent = `Execution ${execution.id} → ${execution.status}`;
        agentPrompt.value = '';
        await loadDashboardData();
    } catch (error) {
        agentMessage.textContent = `Execution failed: ${error.message}`;
    } finally {
        agentSubmitButton.disabled = false;
    }
});

approvalList.addEventListener('click', async event => {
    const button = event.target.closest('button[data-action][data-id]');
    if (!button) return;

    button.disabled = true;
    try {
        await decideApproval(button.dataset.id, button.dataset.action);
    } catch (error) {
        approvalList.insertAdjacentHTML('afterbegin', `<div class="empty-state">Decision failed: ${escapeHtml(error.message)}</div>`);
        button.disabled = false;
    }
});

document.getElementById('logoutButton').addEventListener('click', logout);
document.getElementById('refreshButton').addEventListener('click', async () => {
    await checkHealth();
    await loadDashboardData();
});

checkHealth();
loadDashboardData();
