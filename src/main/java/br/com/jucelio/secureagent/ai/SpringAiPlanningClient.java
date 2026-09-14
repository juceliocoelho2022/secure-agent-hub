package br.com.jucelio.secureagent.ai;

import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.stream.Collectors;

public class SpringAiPlanningClient implements AiPlanningClient {
    private final ChatClient chatClient;
    private final ToolCatalog toolCatalog;
    private final BeanOutputConverter<AiToolProposal> outputConverter;

    public SpringAiPlanningClient(ChatClient chatClient, ToolCatalog toolCatalog) {
        this.chatClient = chatClient;
        this.toolCatalog = toolCatalog;
        this.outputConverter = new BeanOutputConverter<>(AiToolProposal.class);
    }

    @Override
    public AiPlanningResult propose(String prompt) {
        ChatResponse response = chatClient.prompt()
                .system(systemPrompt())
                .user(prompt)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("Spring AI returned no chat response");
        }

        AiToolProposal proposal = outputConverter.convert(response.getResult().getOutput().getText());
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();

        AiUsageMetadata usageMetadata = usage == null
                ? AiUsageMetadata.unknown()
                : new AiUsageMetadata(
                        usage.getPromptTokens(),
                        usage.getCompletionTokens(),
                        usage.getTotalTokens());

        return new AiPlanningResult(proposal, usageMetadata);
    }

    String systemPrompt() {
        String tools = toolCatalog.allowedToolNames().stream()
                .sorted()
                .collect(Collectors.joining(", "));

        return """
                You are an intent planner, not an executor.
                Return exactly one backend tool proposal as structured output.
                Never execute tools and never claim that an action was executed.
                Choose only from the allowed backend tools.
                Allowed tools: %s.
                The backend Policy Engine decides whether the proposal is allowed,
                denied, or requires human approval.

                %s
                """.formatted(tools, outputConverter.getFormat());
    }
}
