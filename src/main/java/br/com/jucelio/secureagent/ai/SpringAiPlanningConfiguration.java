package br.com.jucelio.secureagent.ai;

import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.agent",
        name = "planner",
        havingValue = "spring-ai")
public class SpringAiPlanningConfiguration {

    @Bean
    ChatClient secureAgentPlanningChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    AiPlanningClient aiPlanningClient(ChatClient secureAgentPlanningChatClient,
                                      ToolCatalog toolCatalog) {
        return new SpringAiPlanningClient(
                secureAgentPlanningChatClient,
                toolCatalog);
    }
}
