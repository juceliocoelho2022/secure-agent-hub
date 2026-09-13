const plannerTelemetry = document.getElementById('plannerTelemetry');

function renderPlannerTelemetry(execution) {
    if (!plannerTelemetry) return;

    const plannerSource = execution?.plannerSource || 'UNKNOWN';
    const hasProviderUsage = execution?.promptTokens != null
        || execution?.completionTokens != null
        || execution?.totalTokens != null;

    const usageText = hasProviderUsage
        ? `Provider usage: prompt ${execution.promptTokens ?? 'n/a'} · completion ${execution.completionTokens ?? 'n/a'} · total ${execution.totalTokens ?? 'n/a'}`
        : 'Provider usage: n/a';

    plannerTelemetry.innerHTML = `
        <div class="decision-summary-title">Planner Telemetry</div>
        <div class="decision-grid">
            <div><span>Planner</span><strong>${escapeHtml(plannerSource)}</strong></div>
            <div><span>Token usage</span><strong>${escapeHtml(usageText)}</strong></div>
        </div>`;
}

const governedInspectExecution = window.inspectExecution;
if (typeof governedInspectExecution === 'function') {
    window.inspectExecution = async function (item) {
        await governedInspectExecution(item);
        renderPlannerTelemetry(item);
    };
}

if (plannerTelemetry) {
    plannerTelemetry.innerHTML = `
        <div class="decision-summary-title">Planner Telemetry</div>
        <div class="empty-state">Select an execution to inspect planner and provider usage.</div>`;
}
