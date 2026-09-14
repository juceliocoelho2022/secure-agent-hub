const agentStatusPanel = document.getElementById('agentStatusPanel');
const executionStatusChart = document.getElementById('executionStatusChart');
const plannerUsageChart = document.getElementById('plannerUsageChart');
const approvalPressure = document.getElementById('approvalPressure');

function renderCommandCenterAnalytics() {
    const executions = Array.isArray(state.executions) ? state.executions : [];
    renderAgentStatus(executions);
    renderExecutionStatus(executions);
    renderPlannerUsage(executions);
    renderApprovalPressure(executions);
}

function renderAgentStatus(executions) {
    if (!agentStatusPanel) return;

    const agents = [
        { id: 'fraud-agent', label: 'Fraud Agent', icon: 'F' },
        { id: 'operations-agent', label: 'Operations Agent', icon: 'O' }
    ];

    agentStatusPanel.innerHTML = agents.map(agent => {
        const ownExecutions = executions.filter(item => item.agent === agent.id);
        const latest = ownExecutions[0];
        const status = latest?.status || 'n/a';
        const stateClass = agentStateClass(status);
        const detail = ownExecutions.length
            ? `${ownExecutions.length} execution${ownExecutions.length === 1 ? '' : 's'} · latest ${status}`
            : 'n/a · no observed executions';

        return `
            <div class="live-agent-row">
                <div class="agent-avatar">${agent.icon}</div>
                <div class="live-agent-copy">
                    <strong>${agent.label}</strong>
                    <span>${detail}</span>
                </div>
                <span class="agent-health ${stateClass}">${status}</span>
            </div>`;
    }).join('');
}

function renderExecutionStatus(executions) {
    if (!executionStatusChart) return;

    if (!executions.length) {
        executionStatusChart.innerHTML = '<div class="analytics-empty">n/a · no execution data</div>';
        return;
    }

    const groups = [
        ['COMPLETED', countStatus(executions, 'COMPLETED')],
        ['WAITING_APPROVAL', countStatus(executions, 'WAITING_APPROVAL')],
        ['REJECTED', executions.filter(item => ['REJECTED', 'DENIED', 'FAILED'].includes(String(item.status || '').toUpperCase())).length]
    ];

    renderBarChart(executionStatusChart, groups);
}

function renderPlannerUsage(executions) {
    if (!plannerUsageChart) return;

    const withPlanner = executions.filter(item => item.plannerSource);
    if (!withPlanner.length) {
        plannerUsageChart.innerHTML = '<div class="analytics-empty">n/a · planner provenance unavailable</div>';
        return;
    }

    const groups = [
        ['RULE_BASED', withPlanner.filter(item => item.plannerSource === 'RULE_BASED').length],
        ['SPRING_AI', withPlanner.filter(item => item.plannerSource === 'SPRING_AI').length]
    ];

    renderBarChart(plannerUsageChart, groups);
}

function renderApprovalPressure(executions) {
    if (!approvalPressure) return;

    if (!executions.length) {
        approvalPressure.innerHTML = '<strong>n/a</strong><span>No execution data</span>';
        approvalPressure.style.setProperty('--pressure', '0deg');
        return;
    }

    const waiting = countStatus(executions, 'WAITING_APPROVAL');
    const ratio = waiting / executions.length;
    const percent = Math.round(ratio * 100);
    const level = percent >= 50 ? 'HIGH' : percent >= 20 ? 'ELEVATED' : 'LOW';

    approvalPressure.style.setProperty('--pressure', `${Math.round(ratio * 360)}deg`);
    approvalPressure.innerHTML = `
        <strong>${percent}%</strong>
        <span>${waiting} waiting · ${level}</span>`;
}

function renderBarChart(target, groups) {
    const max = Math.max(...groups.map(([, count]) => count), 1);
    target.innerHTML = groups.map(([label, count]) => {
        const width = Math.round((count / max) * 100);
        return `
            <div class="analytics-bar-row">
                <div class="analytics-bar-label"><span>${label}</span><strong>${count}</strong></div>
                <div class="analytics-track"><span style="width:${width}%"></span></div>
            </div>`;
    }).join('');
}

function countStatus(executions, status) {
    return executions.filter(item => String(item.status || '').toUpperCase() === status).length;
}

function agentStateClass(status) {
    const value = String(status || '').toUpperCase();
    if (value === 'COMPLETED') return 'healthy';
    if (value === 'WAITING_APPROVAL') return 'waiting';
    if (['REJECTED', 'DENIED', 'FAILED'].includes(value)) return 'attention';
    return 'neutral';
}

const analyticsObserver = new MutationObserver(() => renderCommandCenterAnalytics());
const executionCountNode = document.getElementById('executionCount');
const approvalCountNode = document.getElementById('approvalCount');
if (executionCountNode) analyticsObserver.observe(executionCountNode, { childList: true, characterData: true, subtree: true });
if (approvalCountNode) analyticsObserver.observe(approvalCountNode, { childList: true, characterData: true, subtree: true });

renderCommandCenterAnalytics();
