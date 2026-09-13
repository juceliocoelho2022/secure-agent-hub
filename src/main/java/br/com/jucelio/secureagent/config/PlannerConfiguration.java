package br.com.jucelio.secureagent.config;

import br.com.jucelio.secureagent.ai.AiPlanningClient;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.RuleBasedAgentPlanner;
import br.com.jucelio.secureagent.tool.SpringAiAgentPlanner;
import br.com.jucelio.secureagent.tool.ToolCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PlannerConfiguration {

    @Bean
    ToolCatalog toolCatalog() {
        return new ToolCatalog();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.agent",
            name = "planner",
            havingValue = "rules",
            matchIfMissing = true)
    AgentPlanner ruleBasedAgentPlanner() {
        return new RuleBasedAgentPlanner();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.agent",
            name = "planner",
            havingValue = "spring-ai")
    AgentPlanner springAiAgentPlanner(AiPlanningClient aiPlanningClient,
                                      ToolCatalog toolCatalog) {
        return new SpringAiAgentPlanner(
                aiPlanningClient,
                toolCatalog,
                new RuleBasedAgentPlanner());
    }
}
