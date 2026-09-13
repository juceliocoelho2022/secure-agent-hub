package br.com.jucelio.secureagent.ai;

public record AiPlanningResult(
        AiToolProposal proposal,
        AiUsageMetadata usage
) {
    public AiPlanningResult {
        if (proposal == null) {
            throw new IllegalArgumentException("proposal is required");
        }
        usage = usage == null ? AiUsageMetadata.unknown() : usage;
    }
}
