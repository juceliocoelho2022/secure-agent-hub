(() => {
    const riskOverview = document.getElementById('riskOverview');
    const recommendedAction = document.getElementById('recommendedAction');
    const eventPipelineHealth = document.getElementById('eventPipelineHealth');

    function renderRisk() {
        if (!riskOverview) return;
        const executions = Array.isArray(state?.executions) ? state.executions : [];
        const candidate = [...executions].reverse().find(item =>
            Number.isInteger(item?.riskScore) && item?.riskLevel && Array.isArray(item?.riskReasons));

        if (!candidate) {
            riskOverview.innerHTML = '<strong>n/a</strong><span>No trusted risk assessment available</span><small>Run calculateRisk with structured context.</small>';
            return;
        }

        const reasons = candidate.riskReasons.length
            ? candidate.riskReasons.map(reason => `<span class="risk-reason">${reason}</span>`).join('')
            : '<span class="risk-reason neutral">No triggered risk reasons</span>';

        riskOverview.innerHTML = `
            <div class="risk-score-row"><strong>${candidate.riskScore}<small>/100</small></strong><span class="risk-level ${String(candidate.riskLevel).toLowerCase()}">${candidate.riskLevel}</span></div>
            <div class="risk-reasons">${reasons}</div>
            <small>Execution ${candidate.id || candidate.entityId || 'observed'} · backend-calculated</small>`;
    }

    function renderRecommendation() {
        if (!recommendedAction) return;
        const executions = Array.isArray(state?.executions) ? state.executions : [];
        const candidate = [...executions].reverse().find(item => item?.recommendedAction && item?.recommendationReason);
        if (!candidate) {
            recommendedAction.innerHTML = '<strong>n/a</strong><span>No governed recommendation</span><small>Critical risk may recommend action, never auto-execute it.</small>';
            return;
        }

        const status = String(candidate.status || '').toUpperCase();
        const human = status === 'WAITING_APPROVAL' ? 'PENDING' : status === 'REJECTED' ? 'REJECTED' : status === 'COMPLETED' ? 'APPROVED' : 'n/a';
        const execution = status === 'COMPLETED' ? 'EXECUTED' : status === 'REJECTED' ? 'NOT_EXECUTED' : 'NOT_EXECUTED';
        recommendedAction.innerHTML = `
            <div class="risk-score-row"><strong>${candidate.recommendedAction}</strong><span class="risk-level critical">${candidate.recommendationReason}</span></div>
            <div class="risk-reasons">
                <span class="risk-reason">Risk ${candidate.riskScore ?? 'n/a'} / ${candidate.riskLevel ?? 'n/a'}</span>
                <span class="risk-reason">Policy REQUIRE_APPROVAL</span>
                <span class="risk-reason">Human ${human}</span>
                <span class="risk-reason">Execution ${execution}</span>
            </div>
            <small>Policy governed · backend persisted recommendation</small>`;
    }

    function pipelineNode(label, value, status) {
        const css = status === 'HEALTHY' || status === 'ACTIVE' ? 'healthy' : status === 'BACKLOG' ? 'warning' : 'neutral';
        return `<div class="pipeline-node ${css}"><span>${label}</span><strong>${value}</strong><small>${status || 'n/a'}</small></div>`;
    }

    async function renderPipeline() {
        if (!eventPipelineHealth) return;
        if (!state?.accessToken) {
            eventPipelineHealth.innerHTML = pipelineNode('Outbox', 'n/a', 'n/a') + pipelineNode('Kafka', 'n/a', 'n/a') + pipelineNode('Consumer', 'n/a', 'n/a') + pipelineNode('DLT', 'n/a', 'n/a');
            return;
        }
        try {
            const data = await api('/api/v1/operations/pipeline-health');
            eventPipelineHealth.innerHTML = pipelineNode('Outbox', data.pendingOutbox ?? 'n/a', data.outboxStatus) + pipelineNode('Kafka', 'n/a', data.kafkaStatus) + pipelineNode('Consumer', data.processedEvents ?? 'n/a', data.consumerStatus) + pipelineNode('DLT', data.deadLetterEvents ?? 'n/a', data.dltStatus);
        } catch (_) {
            eventPipelineHealth.innerHTML = pipelineNode('Outbox', 'n/a', 'n/a') + pipelineNode('Kafka', 'n/a', 'n/a') + pipelineNode('Consumer', 'n/a', 'n/a') + pipelineNode('DLT', 'n/a', 'n/a');
        }
    }

    function refreshOperationalIntelligence() { renderRisk(); renderRecommendation(); renderPipeline(); }
    document.addEventListener('DOMContentLoaded', refreshOperationalIntelligence);
    const executionCount = document.getElementById('executionCount');
    if (executionCount) new MutationObserver(refreshOperationalIntelligence).observe(executionCount, { childList: true, subtree: true, characterData: true });
})();
