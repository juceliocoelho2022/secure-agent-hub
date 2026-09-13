package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.common.BaseEntity;
import br.com.jucelio.secureagent.tool.AgentPlan;
import br.com.jucelio.secureagent.tool.PlannerSource;
import jakarta.persistence.*;

@Entity
@Table(name = "agent_executions")
public class AgentExecution extends BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private java.util.UUID entityId;

    @Column(nullable = false, length = 80)
    private String agentName;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExecutionStatus status;

    @Column(length = 120)
    private String requestedTool;

    @Column(length = 4000)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "planner_source", length = 32)
    private PlannerSource plannerSource;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    protected AgentExecution() {}

    public AgentExecution(String agentName, String prompt) {
        this.entityId = java.util.UUID.randomUUID();
        this.id = entityId;
        this.agentName = agentName;
        this.prompt = prompt;
        this.status = ExecutionStatus.RECEIVED;
    }

    public java.util.UUID getEntityId() { return entityId; }
    public String getAgentName() { return agentName; }
    public String getPrompt() { return prompt; }
    public ExecutionStatus getStatus() { return status; }
    public String getRequestedTool() { return requestedTool; }
    public String getResult() { return result; }
    public PlannerSource getPlannerSource() { return plannerSource; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }

    public void start() { this.status = ExecutionStatus.RUNNING; }

    public void recordPlanning(AgentPlan plan) {
        this.plannerSource = plan.source();
        this.promptTokens = plan.usage().promptTokens();
        this.completionTokens = plan.usage().completionTokens();
        this.totalTokens = plan.usage().totalTokens();
    }

    public void waitForApproval(String requestedTool) {
        this.requestedTool = requestedTool;
        this.status = ExecutionStatus.WAITING_APPROVAL;
    }
    public void complete(String result) { this.result = result; this.status = ExecutionStatus.COMPLETED; }
    public void reject(String result) { this.result = result; this.status = ExecutionStatus.REJECTED; }
}
