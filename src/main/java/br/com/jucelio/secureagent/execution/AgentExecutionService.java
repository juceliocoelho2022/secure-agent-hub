package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.approval.ApprovalRequest;
import br.com.jucelio.secureagent.approval.ApprovalRequestRepository;
import br.com.jucelio.secureagent.audit.AuditService;
import br.com.jucelio.secureagent.event.DomainEventService;
import br.com.jucelio.secureagent.policy.PolicyDecision;
import br.com.jucelio.secureagent.policy.PolicyService;
import br.com.jucelio.secureagent.risk.ExplainableRiskService;
import br.com.jucelio.secureagent.risk.RiskAssessment;
import br.com.jucelio.secureagent.risk.RiskAssessmentRequest;
import br.com.jucelio.secureagent.risk.RiskRecommendation;
import br.com.jucelio.secureagent.risk.RiskRecommendationService;
import br.com.jucelio.secureagent.tool.AgentPlan;
import br.com.jucelio.secureagent.tool.AgentPlanner;
import br.com.jucelio.secureagent.tool.ToolExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentExecutionService {
    private final AgentExecutionRepository repository;
    private final ApprovalRequestRepository approvalRepository;
    private final AgentPlanner planner;
    private final PolicyService policyService;
    private final ToolExecutor toolExecutor;
    private final AuditService auditService;
    private final DomainEventService domainEventService;
    private final ExplainableRiskService riskService;
    private final RiskRecommendationService recommendationService;

    public AgentExecutionService(AgentExecutionRepository repository,
                                 ApprovalRequestRepository approvalRepository,
                                 AgentPlanner planner,
                                 PolicyService policyService,
                                 ToolExecutor toolExecutor,
                                 AuditService auditService,
                                 DomainEventService domainEventService,
                                 ExplainableRiskService riskService,
                                 RiskRecommendationService recommendationService) {
        this.repository = repository;
        this.approvalRepository = approvalRepository;
        this.planner = planner;
        this.policyService = policyService;
        this.toolExecutor = toolExecutor;
        this.auditService = auditService;
        this.domainEventService = domainEventService;
        this.riskService = riskService;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public AgentExecution create(CreateExecutionRequest request, String username) {
        AgentExecution execution = repository.save(new AgentExecution(request.agent(), request.prompt()));
        auditService.record(execution.getEntityId(), username, "EXECUTION_CREATED", "Agent=" + request.agent());
        domainEventService.append(execution.getEntityId(), "agent.execution.created", Map.of(
                "executionId", execution.getEntityId().toString(), "agent", request.agent(), "requestedBy", username));
        execution.start();

        AgentPlan plan = planner.plan(request.prompt());
        execution.recordPlanning(plan);
        String usageDetails = plan.usage().available()
                ? " usage=prompt:" + plan.usage().promptTokens() + ",completion:" + plan.usage().completionTokens() + ",total:" + plan.usage().totalTokens()
                : "";
        auditService.record(execution.getEntityId(), "AI_AGENT", "TOOL_PLANNED",
                plan.toolName() + " - " + plan.explanation() + " source=" + plan.source().name() + usageDetails);

        Map<String, Object> plannedEvent = new HashMap<>();
        plannedEvent.put("executionId", execution.getEntityId().toString());
        plannedEvent.put("tool", plan.toolName());
        plannedEvent.put("explanation", plan.explanation());
        plannedEvent.put("source", plan.source().name());
        if (plan.usage().promptTokens() != null) plannedEvent.put("promptTokens", plan.usage().promptTokens());
        if (plan.usage().completionTokens() != null) plannedEvent.put("completionTokens", plan.usage().completionTokens());
        if (plan.usage().totalTokens() != null) plannedEvent.put("totalTokens", plan.usage().totalTokens());
        domainEventService.append(execution.getEntityId(), "agent.tool.planned", plannedEvent);

        PolicyDecision decision = policyService.evaluate(plan.toolName());
        if (!decision.allowed()) {
            execution.reject("Denied by policy: " + decision.reason());
            auditService.record(execution.getEntityId(), "POLICY_ENGINE", "TOOL_DENIED", decision.reason());
            domainEventService.append(execution.getEntityId(), "agent.tool.denied", Map.of(
                    "executionId", execution.getEntityId().toString(), "tool", plan.toolName(), "reason", decision.reason()));
            return repository.save(execution);
        }
        if (decision.humanApprovalRequired()) return requestApproval(execution, plan.toolName(), decision.reason());
        if ("calculateRisk".equals(plan.toolName())) return executeRiskAssessment(execution, request.context());

        String result = toolExecutor.execute(plan.toolName());
        execution.complete(result);
        auditService.record(execution.getEntityId(), "TOOL_SERVICE", "TOOL_EXECUTED", result);
        domainEventService.append(execution.getEntityId(), "agent.execution.completed", Map.of(
                "executionId", execution.getEntityId().toString(), "tool", plan.toolName(), "result", result));
        return repository.save(execution);
    }

    private AgentExecution executeRiskAssessment(AgentExecution execution, ExecutionContext context) {
        if (context == null) {
            String result = "Risk assessment unavailable: structured context required.";
            execution.complete(result);
            auditService.record(execution.getEntityId(), "RISK_ENGINE", "RISK_CONTEXT_MISSING", result);
            domainEventService.append(execution.getEntityId(), "agent.risk.unavailable", Map.of(
                    "executionId", execution.getEntityId().toString(), "reason", "STRUCTURED_CONTEXT_REQUIRED"));
            return repository.save(execution);
        }

        RiskAssessment assessment = riskService.assess(new RiskAssessmentRequest(
                context.transactionId(), context.amount(), context.country(), context.usualCountry(), context.hour(), context.rapidRetry(), context.knownDevice()));
        execution.recordRisk(assessment);
        String riskResult = "riskScore=" + assessment.score() + "; riskLevel=" + assessment.level().name() + "; reasons=" + assessment.reasons();

        auditService.record(execution.getEntityId(), "RISK_ENGINE", "RISK_ASSESSED",
                "score=" + assessment.score() + " level=" + assessment.level().name() + " reasons=" + assessment.reasons());
        Map<String, Object> riskEvent = new HashMap<>();
        riskEvent.put("executionId", execution.getEntityId().toString());
        if (context.transactionId() != null) riskEvent.put("transactionId", context.transactionId());
        riskEvent.put("riskScore", assessment.score());
        riskEvent.put("riskLevel", assessment.level().name());
        riskEvent.put("reasons", assessment.reasons().stream().map(Enum::name).toList());
        domainEventService.append(execution.getEntityId(), "agent.risk.assessed", riskEvent);

        var recommendation = recommendationService.recommend(assessment);
        if (recommendation.isPresent()) {
            RiskRecommendation rec = recommendation.get();
            execution.recordRecommendation(rec);
            auditService.record(execution.getEntityId(), "RISK_ENGINE", "RISK_ACTION_RECOMMENDED",
                    "action=" + rec.action() + " reason=" + rec.reason() + " score=" + assessment.score());
            domainEventService.append(execution.getEntityId(), "agent.risk.action-recommended", Map.of(
                    "executionId", execution.getEntityId().toString(),
                    "riskScore", assessment.score(),
                    "riskLevel", assessment.level().name(),
                    "recommendedAction", rec.action(),
                    "reason", rec.reason()));

            PolicyDecision recommendationDecision = policyService.evaluate(rec.action());
            if (!recommendationDecision.allowed()) {
                execution.reject("Recommended action denied by policy: " + recommendationDecision.reason());
                return repository.save(execution);
            }
            if (recommendationDecision.humanApprovalRequired()) {
                return requestApproval(execution, rec.action(), recommendationDecision.reason());
            }
        }

        execution.complete(riskResult);
        domainEventService.append(execution.getEntityId(), "agent.execution.completed", Map.of(
                "executionId", execution.getEntityId().toString(), "tool", "calculateRisk", "result", riskResult));
        return repository.save(execution);
    }

    private AgentExecution requestApproval(AgentExecution execution, String toolName, String reason) {
        execution.waitForApproval(toolName);
        approvalRepository.save(new ApprovalRequest(execution.getEntityId(), toolName, reason));
        auditService.record(execution.getEntityId(), "POLICY_ENGINE", "HUMAN_APPROVAL_REQUIRED", reason);
        domainEventService.append(execution.getEntityId(), "agent.approval.requested", Map.of(
                "executionId", execution.getEntityId().toString(), "tool", toolName, "reason", reason));
        return repository.save(execution);
    }

    @Transactional(readOnly = true)
    public AgentExecution get(UUID id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Execution not found")); }
    @Transactional(readOnly = true)
    public List<AgentExecution> list() { return repository.findAll(); }

    @Transactional
    public AgentExecution executeApprovedTool(UUID executionId, String toolName, String operator) {
        AgentExecution execution = get(executionId);
        String result = toolExecutor.execute(toolName);
        execution.complete(result);
        auditService.record(executionId, operator, "APPROVED_TOOL_EXECUTED", toolName + " -> " + result);
        domainEventService.append(executionId, "agent.tool.approved-and-executed", Map.of(
                "executionId", executionId.toString(), "tool", toolName, "approvedBy", operator, "result", result));
        return repository.save(execution);
    }

    @Transactional
    public AgentExecution markRejected(UUID executionId, String operator) {
        AgentExecution execution = get(executionId);
        execution.reject("Rejected by human operator");
        auditService.record(executionId, operator, "HUMAN_REJECTED", "Operation rejected by human operator");
        domainEventService.append(executionId, "agent.approval.rejected", Map.of(
                "executionId", executionId.toString(), "rejectedBy", operator));
        return repository.save(execution);
    }
}
