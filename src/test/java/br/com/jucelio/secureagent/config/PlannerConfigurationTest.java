package br.com.jucelio.secureagent.config;

import br.com.jucelio.secureagent.ai.AiPlanningClient;
import br.com.jucelio.secureagent.ai.AiPlanningResult;
import br.com.jucelio.secureagent.ai.AiToolProposal;
import br.com.jucelio.secureagent.ai.AiUsageMetadata;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.RuleBasedAgentPlanner;
import br.com.jucelio.secureagent.tool.SpringAiAgentPlanner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PlannerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void rulesPlannerIsTheDefaultAndOnlyActivePlanner() {
        contextRunner
                .withUserConfiguration(PlannerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentPlanner.class);
                    assertThat(context.getBean(AgentPlanner.class))
                            .isInstanceOf(RuleBasedAgentPlanner.class);
                });
    }

    @Test
    void springAiModeSelectsOnlyTheControlledAiPlanner() {
        contextRunner
                .withUserConfiguration(
                        PlannerConfiguration.class,
                        StubAiPlanningConfiguration.class)
                .withPropertyValues("app.agent.planner=spring-ai")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentPlanner.class);
                    assertThat(context.getBean(AgentPlanner.class))
                            .isInstanceOf(SpringAiAgentPlanner.class);
                });
    }

    @Test
    void automaticSpringAiToolCallingIsExplicitlyDisabled() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/application.yml")) {
            assertThat(input).isNotNull();
            String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("tool-calling:");
            assertThat(yaml).contains("enabled: false");
            assertThat(yaml).contains("planner: ${AGENT_PLANNER:rules}");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StubAiPlanningConfiguration {
        @Bean
        AiPlanningClient aiPlanningClient() {
            return prompt -> new AiPlanningResult(
                    new AiToolProposal("calculateRisk", "stub"),
                    AiUsageMetadata.unknown());
        }
    }
}
