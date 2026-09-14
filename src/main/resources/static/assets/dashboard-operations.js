(() => {
    const riskOverview = document.getElementById('riskOverview');
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
            <div class="risk-score-row">
                <strong>${candidate.riskScore}<small>/100</small></strong>
                <span class="risk-level ${String(candidate.riskLevel).toLowerCase()}">${candidate.riskLevel}</span>
            </div>
            <div class="risk-reasons">${reasons}</div>
            <small>Execution ${candidate.id || candidate.entityId || 'observed'} · backend-calculated</small>`;
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
            eventPipelineHealth.innerHTML =
                pipelineNode('Outbox', data.pendingOutbox ?? 'n/a', data.outboxStatus) +
                pipelineNode('Kafka', 'n/a', data.kafkaStatus) +
                pipelineNode('Consumer', data.processedEvents ?? 'n/a', data.consumerStatus) +
                pipelineNode('DLT', data.deadLetterEvents ?? 'n/a', data.dltStatus);
        } catch (_) {
            eventPipelineHealth.innerHTML = pipelineNode('Outbox', 'n/a', 'n/a') + pipelineNode('Kafka', 'n/a', 'n/a') + pipelineNode('Consumer', 'n/a', 'n/a') + pipelineNode('DLT', 'n/a', 'n/a');
        }
    }

    function refreshOperationalIntelligence() {
        renderRisk();
        renderPipeline();
    }

    document.addEventListener('DOMContentLoaded', refreshOperationalIntelligence);

    const executionCount = document.getElementById('executionCount');
    if (executionCount) {
        new MutationObserver(refreshOperationalIntelligence).observe(executionCount, { childList: true, subtree: true, characterData: true });
    }
})();
