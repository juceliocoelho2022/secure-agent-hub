package br.com.jucelio.secureagent.ai;

import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.springframework.ai.chat.client.ChatClient;

import java.util.stream.Collectors;

public class SpringAiPlanningClient implements AiPlanningClient {
    private final ChatClient chatClient;
    private final ToolCatalog toolCatalog;

    public SpringAiPlanningClient(ChatClient chatClient, ToolCatalog toolCatalog) {
        this.chatClient = chatClient;
        this.toolCatalog = toolCatalog;
    }

    @Override
    public AiToolProposal propose(String prompt) {
        return chatClient.prompt()
                .system(systemPrompt())
                .user(prompt)
                .call()
                .entity(AiToolProposal.class);
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
                """.formatted(tools);
    }
}
