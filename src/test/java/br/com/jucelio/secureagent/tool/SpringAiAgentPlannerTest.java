package br.com.jucelio.secureagent.tool;

import br.com.jucelio.secureagent.ai.AiPlanningClient;
import br.com.jucelio.secureagent.ai.AiPlanningResult;
import br.com.jucelio.secureagent.ai.AiToolProposal;
import br.com.jucelio.secureagent.ai.AiUsageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpringAiAgentPlannerTest {
    private AiPlanningClient client;
    private AgentPlanner fallback;
    private SpringAiAgentPlanner planner;

    @BeforeEach
    void setup() {
        client = mock(AiPlanningClient.class);
        fallback = mock(AgentPlanner.class);
        planner = new SpringAiAgentPlanner(client, new ToolCatalog(), fallback);
    }

    @Test
    void returnsSpringAiPlanForAllowedStructuredProposal() {
        when(client.propose("fraud prompt"))
                .thenReturn(new AiPlanningResult(
                        new AiToolProposal("blockCard", "fraud detected"),
                        new AiUsageMetadata(12, 4, 16)));

        AgentPlan result = planner.plan("fraud prompt");

        assertEquals("blockCard", result.toolName());
        assertEquals("fraud detected", result.explanation());
        assertEquals(PlannerSource.SPRING_AI, result.source());
        verify(fallback, never()).plan(anyString());
    }

    @Test
    void fallsBackWhenAiSuggestsUnsupportedTool() {
        when(client.propose(anyString()))
                .thenReturn(new AiPlanningResult(
                        new AiToolProposal("runShellCommand", "do it"),
                        AiUsageMetadata.unknown()));
        when(fallback.plan(anyString()))
                .thenReturn(new AgentPlan(
                        "calculateRisk",
                        "fallback",
                        PlannerSource.RULE_BASED));

        AgentPlan result = planner.plan("prompt");

        assertEquals(PlannerSource.RULE_BASED, result.source());
        assertEquals("calculateRisk", result.toolName());
        verify(fallback).plan("prompt");
    }

    @Test
    void fallsBackWhenProviderFails() {
        when(client.propose(anyString()))
                .thenThrow(new RuntimeException("provider unavailable"));
        when(fallback.plan(anyString()))
                .thenReturn(new AgentPlan(
                        "calculateRisk",
                        "fallback",
                        PlannerSource.RULE_BASED));

        AgentPlan result = planner.plan("prompt");

        assertEquals(PlannerSource.RULE_BASED, result.source());
        verify(fallback).plan("prompt");
    }

    @Test
    void fallsBackWhenProviderReturnsNullResult() {
        when(client.propose(anyString())).thenReturn(null);
        when(fallback.plan(anyString()))
                .thenReturn(new AgentPlan(
                        "calculateRisk",
                        "fallback",
                        PlannerSource.RULE_BASED));

        AgentPlan result = planner.plan("prompt");

        assertEquals(PlannerSource.RULE_BASED, result.source());
        verify(fallback).plan("prompt");
    }
}
