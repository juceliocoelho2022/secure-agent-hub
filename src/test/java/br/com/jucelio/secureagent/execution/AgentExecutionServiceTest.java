package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.approval.ApprovalRequestRepository;
import br.com.jucelio.secureagent.audit.AuditService;
import br.com.jucelio.secureagent.event.DomainEventService;
import br.com.jucelio.secureagent.policy.PolicyService;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.RuleBasedAgentPlanner;
import br.com.jucelio.secureagent.tool.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentExecutionServiceTest {
    private AgentExecutionRepository executionRepository;
    private ApprovalRequestRepository approvalRepository;
    private AuditService auditService;
    private DomainEventService domainEventService;
    private AgentExecutionService service;

    @BeforeEach
    void setup() {
        executionRepository = mock(AgentExecutionRepository.class);
        approvalRepository = mock(ApprovalRequestRepository.class);
        auditService = mock(AuditService.class);
        domainEventService = mock(DomainEventService.class);
        AgentPlanner planner = new RuleBasedAgentPlanner();
        PolicyService policyService = new PolicyService();
        ToolExecutor toolExecutor = new ToolExecutor();

        when(executionRepository.save(any(AgentExecution.class))).thenAnswer(i -> i.getArgument(0));

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
        verify(domainEventService, atLeastOnce()).record(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCompleteNonCriticalToolAutomatically() {
        AgentExecution result = service.create(
                new CreateExecutionRequest("fraud-agent", "Consulte a transação TX-10"),
                "analyst");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.getResult()).contains("Transaction retrieved");
        verify(domainEventService, atLeastOnce()).record(any(), any(), any(), any(), any());
    }
}
