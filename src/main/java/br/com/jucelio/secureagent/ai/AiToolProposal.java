package br.com.jucelio.secureagent.ai;

public record AiToolProposal(String toolName, String explanation) {
    public AiToolProposal {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName is required");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation is required");
        }
    }
}
