package br.com.jucelio.secureagent.execution;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        String agent,
        String prompt,
        ExecutionStatus status,
        String requestedTool,
        String result,
        Instant createdAt
) {
    static ExecutionResponse from(AgentExecution e) {
        return new ExecutionResponse(
                e.getEntityId(), e.getAgentName(), e.getPrompt(), e.getStatus(),
                e.getRequestedTool(), e.getResult(), e.getCreatedAt());
    }
}
