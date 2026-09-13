package br.com.jucelio.secureagent.tool;

public record AgentPlan(
        String toolName,
        String explanation,
        PlannerSource source
) {}
