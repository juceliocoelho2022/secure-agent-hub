package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.tool.PlannerSource;

import java.time.Instant;
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
        Integer totalTokens
) {
    static ExecutionResponse from(AgentExecution e) {
        return new ExecutionResponse(
                e.getEntityId(),
                e.getAgentName(),
                e.getPrompt(),
                e.getStatus(),
                e.getRequestedTool(),
                e.getResult(),
                e.getCreatedAt(),
                e.getPlannerSource(),
                e.getPromptTokens(),
                e.getCompletionTokens(),
                e.getTotalTokens());
    }
}
