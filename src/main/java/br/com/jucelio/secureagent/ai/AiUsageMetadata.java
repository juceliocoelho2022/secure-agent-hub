package br.com.jucelio.secureagent.ai;

public record AiUsageMetadata(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
    public static AiUsageMetadata unknown() {
        return new AiUsageMetadata(null, null, null);
    }

    public boolean available() {
        return promptTokens != null || completionTokens != null || totalTokens != null;
    }
}
