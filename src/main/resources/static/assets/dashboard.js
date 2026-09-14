const state = {
    accessToken: sessionStorage.getItem('secureAgentAccessToken') || null,
    refreshToken: sessionStorage.getItem('secureAgentRefreshToken') || null,
    profile: null,
    canApprove: false,
    canViewTimeline: false,
    executions: [],
    selectedExecutionId: null
};

const connectionPill = document.getElementById('connectionPill');
const connectionText = document.getElementById('connectionText');
const executionCount = document.getElementById('executionCount');
const approvalCount = document.getElementById('approvalCount');
const executionList = document.getElementById('executionList');
const approvalList = document.getElementById('approvalList');
const approvalFeedback = document.getElementById('approvalFeedback');
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
const executionInspectorSummary = document.getElementById('executionInspectorSummary');
const decisionSummary = document.getElementById('decisionSummary');
const executionTimeline = document.getElementById('executionTimeline');

const governedLifecycle = [
    'EXECUTION_CREATED',
    'TOOL_PLANNED',
    'POLICY_EVALUATED',
    'HUMAN_APPROVAL_REQUIRED',
    'HUMAN_APPROVED',
    'TOOL_EXECUTED',
    'COMPLETED'
];

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
    state.canViewTimeline = false;
    state.executions = [];
    state.selectedExecutionId = null;
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
    state.canViewTimeline = authorities.includes('ROLE_OPERATOR') || authorities.includes('ROLE_AUDITOR') || authorities.includes('ROLE_ADMIN');

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
    approvalFeedback.textContent = '';
    executionInspectorSummary.innerHTML = '<div class="empty-state">Authenticate to inspect an execution.</div>';
    decisionSummary.innerHTML = '<div class="decision-summary-title">Decision Summary</div><div class="empty-state">Authenticate to inspect policy decisions.</div>';
    executionTimeline.innerHTML = '<div class="empty-state">Authenticate to load timeline data.</div>';
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
        state.executions = executions;
        executionCount.textContent = executions.length;
        renderExecutions(executions);

        if (state.selectedExecutionId) {
            const selected = executions.find(item => item.id === state.selectedExecutionId);
            if (selected) await inspectExecution(selected);
        }
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

function buildExecutionContext() {
    const transactionId = document.getElementById('riskTransactionId')?.value.trim() || '';
    const amountText = document.getElementById('riskAmount')?.value.trim() || '';
    const country = document.getElementById('riskCountry')?.value.trim().toUpperCase() || '';
    const usualCountry = document.getElementById('riskUsualCountry')?.value.trim().toUpperCase() || '';
    const hourText = document.getElementById('riskHour')?.value.trim() || '';
    const rapidRetry = Boolean(document.getElementById('riskRapidRetry')?.checked);
    const knownDevice = Boolean(document.getElementById('riskKnownDevice')?.checked);

    const hasStructuredFacts = transactionId || amountText || country || usualCountry || hourText || rapidRetry || knownDevice;
    if (!hasStructuredFacts) return null;

    return {
        transactionId: transactionId || null,
        amount: amountText ? Number(amountText) : null,
        country: country || null,
        usualCountry: usualCountry || null,
        hour: hourText ? Number.parseInt(hourText, 10) : null,
        rapidRetry,
        knownDevice
    };
}

async function createExecution(agent, prompt, context) {
    return api('/api/v1/agents/executions', {
        method: 'POST',
        body: JSON.stringify({ agent, prompt, context })
    });
}

async function decideApproval(id, action) {
    const safeId = encodeURIComponent(id);
    const endpoint = action === 'approve'
        ? `/api/v1/approvals/${safeId}/approve`
        : `/api/v1/approvals/${safeId}/reject`;
    await api(endpoint, { method: 'POST' });
    await loadDashboardData();
    approvalFeedback.textContent = action === 'approve'
        ? 'Critical action approved. Execution and timeline refreshed.'
        : 'Critical action rejected. Execution and timeline refreshed.';
}

function renderExecutions(executions) {
    if (!executions.length) {
        executionList.innerHTML = '<div class="empty-state">No executions found.</div>';
        return;
    }

    executionList.innerHTML = executions.slice(0, 5).map(item => {
        const tool = item.requestedTool ? ` · ${item.requestedTool}` : '';
        const detail = item.result || item.prompt || '';
        const selected = item.id === state.selectedExecutionId ? ' selected' : '';
        return `
            <button class="activity-item execution-row${selected}" data-execution-id="${escapeHtml(item.id)}" type="button">
                <div class="activity-main">
                    <strong>${escapeHtml(item.agent || 'agent')}</strong>
                    <div class="activity-meta">${escapeHtml(shortId(item.id))}${escapeHtml(tool)}</div>
                    ${detail ? `<div class="activity-detail">${escapeHtml(detail)}</div>` : ''}
                </div>
                <span class="badge ${statusClass(item.status)}">${escapeHtml(item.status || 'UNKNOWN')}</span>
            </button>`;
    }).join('');
}

function renderApprovals(approvals) {
    if (!approvals.length) {
        approvalList.innerHTML = '<div class="empty-state">No pending approvals.</div>';
        return;
    }

    approvalList.innerHTML = approvals.slice(0, 5).map(item => `
        <div class="approval-item approval-card">
            <div class="approval-context">
                <div class="approval-title-row">
                    <strong>${escapeHtml(item.toolName || 'critical tool')}</strong>
                    <span class="approval-severity">PROTECTED</span>
                </div>
                <div class="activity-meta">Execution ${escapeHtml(shortId(item.executionId || item.id))} · ${escapeHtml(formatTime(item.requestedAt))}</div>
                <div class="approval-reason">${escapeHtml(item.reason || 'Critical operation requires human approval')}</div>
            </div>
            <div class="approval-actions">
                <button class="approval-button approve" data-action="approve" data-id="${escapeHtml(item.id)}" type="button">Approve</button>
                <button class="approval-button reject" data-action="reject" data-id="${escapeHtml(item.id)}" type="button">Reject</button>
            </div>
        </div>`).join('');
}

async function inspectExecution(item) {
    state.selectedExecutionId = item.id;
    renderExecutions(state.executions);
    const riskLine = Number.isInteger(item.riskScore)
        ? `<div class="inspector-kv"><span>Risk</span><strong>${escapeHtml(item.riskScore)}/100 · ${escapeHtml(item.riskLevel || 'n/a')}</strong></div>`
        : '';
    executionInspectorSummary.innerHTML = `
        <div class="inspector-kv"><span>Execution</span><strong>${escapeHtml(shortId(item.id))}</strong></div>
        <div class="inspector-kv"><span>Agent</span><strong>${escapeHtml(item.agent || '—')}</strong></div>
        <div class="inspector-kv"><span>Status</span><strong class="status-text ${statusClass(item.status)}">${escapeHtml(item.status || 'UNKNOWN')}</strong></div>
        <div class="inspector-kv"><span>Requested tool</span><strong>${escapeHtml(item.requestedTool || 'No critical tool')}</strong></div>
        ${riskLine}
        <div class="inspector-block"><span>Intent</span><p>${escapeHtml(item.prompt || '—')}</p></div>
        <div class="inspector-block"><span>Result</span><p>${escapeHtml(item.result || 'Awaiting final result')}</p></div>`;

    renderDecisionSummary(item, []);
    await loadTimeline(item.id);
}

async function loadTimeline(executionId) {
    if (!state.canViewTimeline) {
        executionTimeline.innerHTML = '<div class="empty-state">Operational timeline is limited to Operator, Auditor and Admin roles.</div>';
        return;
    }

    executionTimeline.innerHTML = '<div class="empty-state">Loading governed lifecycle...</div>';
    try {
        const timeline = await api(`/api/v1/agents/executions/${encodeURIComponent(executionId)}/timeline`);
        const items = Array.isArray(timeline) ? timeline : [];
        renderTimeline(items);
        const selected = state.executions.find(item => item.id === executionId);
        if (selected) renderDecisionSummary(selected, items);
    } catch (error) {
        executionTimeline.innerHTML = `<div class="empty-state">Unable to load timeline: ${escapeHtml(error.message)}</div>`;
    }
}

function renderDecisionSummary(execution, timeline) {
    const actions = new Set(timeline.map(item => String(item.action || '').toUpperCase()));
    const status = String(execution.status || '').toUpperCase();
    const approvalRequired = status.includes('APPROVAL') || actions.has('HUMAN_APPROVAL_REQUIRED') || actions.has('HUMAN_APPROVED') || actions.has('HUMAN_REJECTED');
    const rejected = status.includes('REJECTED') || actions.has('HUMAN_REJECTED');
    const approved = actions.has('HUMAN_APPROVED') || actions.has('APPROVED_TOOL_EXECUTED');
    const policyDecision = status.includes('DENIED') ? 'DENY' : approvalRequired ? 'REQUIRE_APPROVAL' : 'ALLOW';
    const humanDecision = rejected ? 'REJECTED' : approved ? 'APPROVED' : approvalRequired ? 'PENDING' : 'NOT_REQUIRED';
    const currentStage = deriveCurrentStage(status, actions);

    decisionSummary.innerHTML = `
        <div class="decision-summary-title">Decision Summary</div>
        <div class="decision-grid">
            <div><span>Policy decision</span><strong class="decision-value ${decisionClass(policyDecision)}">${policyDecision}</strong></div>
            <div><span>Human decision</span><strong class="decision-value ${decisionClass(humanDecision)}">${humanDecision}</strong></div>
            <div><span>Governed tool</span><strong>${escapeHtml(execution.requestedTool || 'None')}</strong></div>
            <div><span>Current stage</span><strong>${escapeHtml(currentStage)}</strong></div>
        </div>`;
}

function deriveCurrentStage(status, actions) {
    if (status.includes('REJECTED') || actions.has('HUMAN_REJECTED')) return 'REJECTED';
    if (status.includes('DENIED')) return 'DENIED';
    if (status.includes('COMPLETED')) return 'COMPLETED';
    if (actions.has('HUMAN_APPROVED')) return 'TOOL_EXECUTION';
    if (status.includes('APPROVAL') || actions.has('HUMAN_APPROVAL_REQUIRED')) return 'HUMAN_APPROVAL';
    if (actions.has('TOOL_PLANNED')) return 'POLICY_EVALUATION';
    return 'EXECUTION';
}

function renderTimeline(items) {
    const normalized = items.map(item => ({ ...item, normalizedAction: String(item.action || '').toUpperCase() }));
    const byAction = new Map(normalized.map(item => [item.normalizedAction, item]));
    const selected = state.executions.find(item => item.id === state.selectedExecutionId) || {};
    const status = String(selected.status || '').toUpperCase();

    if (status.includes('REJECTED') || byAction.has('HUMAN_REJECTED')) {
        const rejectedLifecycle = ['EXECUTION_CREATED', 'TOOL_PLANNED', 'POLICY_EVALUATED', 'HUMAN_APPROVAL_REQUIRED', 'HUMAN_REJECTED', 'REJECTED'];
        executionTimeline.innerHTML = renderLifecycle(rejectedLifecycle, byAction, selected);
        return;
    }

    executionTimeline.innerHTML = renderLifecycle(governedLifecycle, byAction, selected);
}

function renderLifecycle(stages, byAction, execution) {
    const status = String(execution.status || '').toUpperCase();
    const approvalRequired = status.includes('APPROVAL') || byAction.has('HUMAN_APPROVAL_REQUIRED') || byAction.has('HUMAN_APPROVED');
    const policyReached = byAction.has('TOOL_PLANNED') || approvalRequired || status.includes('COMPLETED') || status.includes('DENIED');

    return stages.map((stage, index) => {
        const actual = byAction.get(stage) || lifecycleAlias(stage, byAction);
        const derivedPolicy = stage === 'POLICY_EVALUATED' && policyReached && !actual;
        const derivedTerminal = stage === 'COMPLETED' && status.includes('COMPLETED') && !actual;
        const reached = Boolean(actual || derivedPolicy || derivedTerminal);
        const future = !reached;
        const current = isCurrentStage(stage, status, byAction, stages, index);
        const stateClass = future ? 'future' : current ? 'current' : 'complete';
        const actor = actual?.actor || (derivedPolicy ? 'POLICY_ENGINE' : derivedTerminal ? 'SYSTEM' : 'Pending');
        const time = actual?.createdAt ? formatInstant(actual.createdAt) : reached ? 'Derived from governed state' : 'Awaiting previous stage';

        return `
            <div class="timeline-item ${stateClass}">
                <span class="timeline-dot"></span>
                <div>
                    <strong>${escapeHtml(stage)}</strong>
                    <div class="activity-meta">${escapeHtml(actor)} · ${escapeHtml(time)}</div>
                </div>
            </div>`;
    }).join('');
}

function lifecycleAlias(stage, byAction) {
    if (stage === 'TOOL_EXECUTED') return byAction.get('APPROVED_TOOL_EXECUTED');
    if (stage === 'COMPLETED') return byAction.get('EXECUTION_COMPLETED');
    if (stage === 'REJECTED') return byAction.get('APPROVAL_REJECTED');
    return null;
}

function isCurrentStage(stage, status, byAction, stages, index) {
    if (status.includes('COMPLETED')) return stage === 'COMPLETED';
    if (status.includes('REJECTED')) return stage === 'REJECTED';
    if (status.includes('DENIED')) return stage === 'POLICY_EVALUATED';
    if (status.includes('APPROVAL')) return stage === 'HUMAN_APPROVAL_REQUIRED';
    if (byAction.has('HUMAN_APPROVED')) return stage === 'TOOL_EXECUTED';

    const reachedIndexes = stages
        .map((name, stageIndex) => byAction.has(name) || lifecycleAlias(name, byAction) ? stageIndex : -1)
        .filter(stageIndex => stageIndex >= 0);
    const lastReached = reachedIndexes.length ? Math.max(...reachedIndexes) : 0;
    return index === lastReached;
}

function decisionClass(value) {
    const normalized = String(value || '').toUpperCase();
    if (normalized === 'ALLOW' || normalized === 'APPROVED' || normalized === 'NOT_REQUIRED') return 'positive';
    if (normalized === 'REQUIRE_APPROVAL' || normalized === 'PENDING') return 'warning';
    if (normalized === 'DENY' || normalized === 'REJECTED') return 'negative';
    return '';
}

function shortId(value) {
    const text = String(value || 'unknown');
    return text.length > 14 ? `${text.slice(0, 8)}…${text.slice(-4)}` : text;
}

function formatTime(value) {
    if (!value) return 'time unavailable';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleTimeString('pt-BR');
}

function formatInstant(value) {
    if (!value) return 'time unavailable';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
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
    const context = buildExecutionContext();
    if (!prompt) return;

    agentSubmitButton.disabled = true;
    agentMessage.textContent = 'Submitting governed execution...';
    try {
        const execution = await createExecution(agent, prompt, context);
        state.selectedExecutionId = execution.id;
        agentMessage.textContent = `Execution ${execution.id} → ${execution.status}`;
        agentPrompt.value = '';
        await loadDashboardData();
    } catch (error) {
        agentMessage.textContent = `Execution failed: ${error.message}`;
    } finally {
        agentSubmitButton.disabled = false;
    }
});

executionList.addEventListener('click', async event => {
    const row = event.target.closest('[data-execution-id]');
    if (!row) return;
    const item = state.executions.find(execution => execution.id === row.dataset.executionId);
    if (item) await inspectExecution(item);
});

approvalList.addEventListener('click', async event => {
    const button = event.target.closest('button[data-action][data-id]');
    if (!button) return;

    button.disabled = true;
    approvalFeedback.textContent = button.dataset.action === 'approve' ? 'Approving critical action...' : 'Rejecting critical action...';
    try {
        await decideApproval(button.dataset.id, button.dataset.action);
    } catch (error) {
        approvalFeedback.textContent = `Decision failed: ${error.message}`;
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
