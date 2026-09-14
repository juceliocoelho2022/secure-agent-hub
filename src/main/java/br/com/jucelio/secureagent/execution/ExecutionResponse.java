package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.risk.RiskLevel;
import br.com.jucelio.secureagent.risk.RiskReason;
import br.com.jucelio.secureagent.tool.PlannerSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        String agent,
        String prompt,
        ExecutionStatus status,
        String requestedTool,
        String result,
        Instant createdAt,
        PlannerSource plannerSource,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Integer riskScore,
        RiskLevel riskLevel,
        List<RiskReason> riskReasons,
        String recommendedAction,
        String recommendationReason
) {
    static ExecutionResponse from(AgentExecution e) {
        return new ExecutionResponse(
                e.getEntityId(), e.getAgentName(), e.getPrompt(), e.getStatus(), e.getRequestedTool(), e.getResult(),
                e.getCreatedAt(), e.getPlannerSource(), e.getPromptTokens(), e.getCompletionTokens(), e.getTotalTokens(),
                e.getRiskScore(), e.getRiskLevel(), e.getRiskReasons(), e.getRecommendedAction(), e.getRecommendationReason());
    }
}
