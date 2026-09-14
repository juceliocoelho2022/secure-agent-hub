package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.ai.AiUsageMetadata;
import br.com.jucelio.secureagent.approval.ApprovalRequest;
import br.com.jucelio.secureagent.approval.ApprovalRequestRepository;
import br.com.jucelio.secureagent.audit.AuditService;
import br.com.jucelio.secureagent.event.DomainEventService;
import br.com.jucelio.secureagent.policy.PolicyService;
import br.com.jucelio.secureagent.risk.ExplainableRiskService;
import br.com.jucelio.secureagent.risk.RiskLevel;
import br.com.jucelio.secureagent.risk.RiskReason;
import br.com.jucelio.secureagent.tool.AgentPlan;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.PlannerSource;
import br.com.jucelio.secureagent.tool.RuleBasedAgentPlanner;
import br.com.jucelio.secureagent.tool.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentExecutionServiceTest {
    private AgentExecutionRepository executionRepository;
    private ApprovalRequestRepository approvalRepository;
    private AuditService auditService;
    private DomainEventService domainEventService;
    private AgentPlanner planner;
    private PolicyService policyService;
    private ToolExecutor toolExecutor;
    private ExplainableRiskService riskService;
    private AgentExecutionService service;

    @BeforeEach
    void setup() {
        executionRepository = mock(AgentExecutionRepository.class);
        approvalRepository = mock(ApprovalRequestRepository.class);
        auditService = mock(AuditService.class);
        domainEventService = mock(DomainEventService.class);
        planner = mock(AgentPlanner.class);
        policyService = spy(new PolicyService());
        toolExecutor = spy(new ToolExecutor());
        riskService = spy(new ExplainableRiskService());

        RuleBasedAgentPlanner ruleBasedPlanner = new RuleBasedAgentPlanner();
        when(planner.plan(anyString()))
                .thenAnswer(invocation -> ruleBasedPlanner.plan(invocation.getArgument(0)));
        when(executionRepository.save(any(AgentExecution.class)))
                .thenAnswer(i -> i.getArgument(0));

        service = new AgentExecutionService(
                executionRepository,
                approvalRepository,
                planner,
                policyService,
                toolExecutor,
                auditService,
                domainEventService,
                riskService);
    }

    @Test
    void shouldRequireHumanApprovalForFraudBlock() {
        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "Analise possível fraude e bloqueie o cartão", null),
                "analyst");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.WAITING_APPROVAL);
        assertThat(result.getRequestedTool()).isEqualTo("blockCard");
        verify(approvalRepository).save(any());
        verify(auditService).record(
                eq(result.getEntityId()),
                eq("AI_AGENT"),
                eq("TOOL_PLANNED"),
                contains("source=RULE_BASED"));
        verify(domainEventService, atLeastOnce()).append(any(), anyString(), anyMap());
    }

    @Test
    void shouldCompleteNonCriticalToolAutomatically() {
        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "Consulte a transação TX-10", null),
                "analyst");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.getResult()).contains("Transaction retrieved");
        verify(domainEventService, atLeastOnce()).append(any(), anyString(), anyMap());
    }

    @Test
    void shouldCalculatePersistAndExposeExplainableRiskFromStructuredContext() {
        when(planner.plan(anyString())).thenReturn(new AgentPlan(
                "calculateRisk",
                "Assess structured transaction risk",
                PlannerSource.RULE_BASED,
                AiUsageMetadata.unknown()));

        ExecutionContext context = new ExecutionContext(
                "TX-9001",
                new BigDecimal("9800.00"),
                "US",
                "BR",
                2,
                true,
                false);

        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "Analise a transação TX-9001", context),
                "operator");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.getRiskScore()).isEqualTo(95);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.getRiskReasons()).containsExactly(
                RiskReason.HIGH_AMOUNT,
                RiskReason.FOREIGN_COUNTRY,
                RiskReason.UNUSUAL_HOUR,
                RiskReason.RAPID_RETRY);
        assertThat(result.getResult()).contains("riskScore=95", "riskLevel=CRITICAL");

        ExecutionResponse response = ExecutionResponse.from(result);
        assertThat(response.riskScore()).isEqualTo(95);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(response.riskReasons()).containsExactly(
                RiskReason.HIGH_AMOUNT,
                RiskReason.FOREIGN_COUNTRY,
                RiskReason.UNUSUAL_HOUR,
                RiskReason.RAPID_RETRY);

        verify(riskService).assess(any());
        verify(toolExecutor, never()).execute("calculateRisk");
        verify(auditService).record(
                eq(result.getEntityId()),
                eq("RISK_ENGINE"),
                eq("RISK_ASSESSED"),
                contains("score=95"));
        verify(domainEventService).append(
                eq(result.getEntityId()),
                eq("agent.risk.assessed"),
                argThat(payload -> Integer.valueOf(95).equals(payload.get("riskScore"))));
    }

    @Test
    void springAiBlockCardProposalPersistsPlannerAndUsageAndStillRequiresHumanApproval() {
        when(planner.plan(anyString()))
                .thenReturn(new AgentPlan(
                        "blockCard",
                        "Potential fraud detected",
                        PlannerSource.SPRING_AI,
                        new AiUsageMetadata(21, 9, 30)));

        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "block suspicious card", null),
                "operator");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.WAITING_APPROVAL);
        assertThat(result.getRequestedTool()).isEqualTo("blockCard");
        assertThat(result.getPlannerSource()).isEqualTo(PlannerSource.SPRING_AI);
        assertThat(result.getPromptTokens()).isEqualTo(21);
        assertThat(result.getCompletionTokens()).isEqualTo(9);
        assertThat(result.getTotalTokens()).isEqualTo(30);

        ExecutionResponse response = ExecutionResponse.from(result);
        assertThat(response.plannerSource()).isEqualTo(PlannerSource.SPRING_AI);
        assertThat(response.promptTokens()).isEqualTo(21);
        assertThat(response.completionTokens()).isEqualTo(9);
        assertThat(response.totalTokens()).isEqualTo(30);

        verify(policyService).evaluate("blockCard");
        verify(approvalRepository).save(any(ApprovalRequest.class));
        verify(toolExecutor, never()).execute(anyString());
        verify(auditService).record(
                eq(result.getEntityId()),
                eq("AI_AGENT"),
                eq("TOOL_PLANNED"),
                contains("source=SPRING_AI"));
    }
}
