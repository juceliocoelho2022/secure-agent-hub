(() => {
    const riskOverview = document.getElementById('riskOverview');
    const eventPipelineHealth = document.getElementById('eventPipelineHealth');

    function renderRisk() {
        if (!riskOverview) return;
        const executions = Array.isArray(state?.executions) ? state.executions : [];
        const candidate = [...executions].reverse().find(item => item?.requestedTool === 'calculateRisk' && item?.result);

        if (!candidate || candidate.result.includes('(mock)')) {
            riskOverview.innerHTML = '<strong>n/a</strong><span>No trusted risk score available</span><small>Mock tool output is intentionally excluded.</small>';
            return;
        }

        const match = candidate.result.match(/risk score(?: calculated)?:\s*(\d+(?:\.\d+)?)/i);
        if (!match) {
            riskOverview.innerHTML = '<strong>n/a</strong><span>Risk output unavailable</span><small>No structured score found.</small>';
            return;
        }

        const score = Number(match[1]);
        const level = score >= 70 ? 'HIGH' : score >= 40 ? 'MEDIUM' : 'LOW';
        riskOverview.innerHTML = `<strong>${score}</strong><span>${level} risk</span><small>Execution ${candidate.id || candidate.entityId || 'observed'}</small>`;
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
