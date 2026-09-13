package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.approval.ApprovalRequest;
import br.com.jucelio.secureagent.approval.ApprovalRequestRepository;
import br.com.jucelio.secureagent.audit.AuditService;
import br.com.jucelio.secureagent.event.DomainEventService;
import br.com.jucelio.secureagent.policy.PolicyService;
import br.com.jucelio.secureagent.tool.AgentPlan;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.PlannerSource;
import br.com.jucelio.secureagent.tool.RuleBasedAgentPlanner;
import br.com.jucelio.secureagent.tool.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
                domainEventService);
    }

    @Test
    void shouldRequireHumanApprovalForFraudBlock() {
        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "Analise possível fraude e bloqueie o cartão"),
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
                new CreateExecutionRequest("fraud-agent", "Consulte a transação TX-10"),
                "analyst");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.getResult()).contains("Transaction retrieved");
        verify(domainEventService, atLeastOnce()).append(any(), anyString(), anyMap());
    }

    @Test
    void springAiBlockCardProposalStillRequiresHumanApproval() {
        when(planner.plan(anyString()))
                .thenReturn(new AgentPlan(
                        "blockCard",
                        "Potential fraud detected",
                        PlannerSource.SPRING_AI));

        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "block suspicious card"),
                "operator");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.WAITING_APPROVAL);
        assertThat(result.getRequestedTool()).isEqualTo("blockCard");
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
