package br.com.jucelio.secureagent.tool;

import br.com.jucelio.secureagent.ai.AiUsageMetadata;

public record AgentPlan(
        String toolName,
        String explanation,
        PlannerSource source,
        AiUsageMetadata usage
) {
    public AgentPlan(String toolName, String explanation, PlannerSource source) {
        this(toolName, explanation, source, AiUsageMetadata.unknown());
    }

    public AgentPlan {
        usage = usage == null ? AiUsageMetadata.unknown() : usage;
    }
}
