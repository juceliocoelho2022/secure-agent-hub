package br.com.jucelio.secureagent.tool;

import br.com.jucelio.secureagent.ai.AiPlanningClient;
import br.com.jucelio.secureagent.ai.AiPlanningResult;
import br.com.jucelio.secureagent.ai.AiToolProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringAiAgentPlanner implements AgentPlanner {
    private static final Logger log = LoggerFactory.getLogger(SpringAiAgentPlanner.class);

    private final AiPlanningClient client;
    private final ToolCatalog toolCatalog;
    private final AgentPlanner fallback;

    public SpringAiAgentPlanner(AiPlanningClient client,
                                ToolCatalog toolCatalog,
                                AgentPlanner fallback) {
        this.client = client;
        this.toolCatalog = toolCatalog;
        this.fallback = fallback;
    }

    @Override
    public AgentPlan plan(String prompt) {
        try {
            AiPlanningResult planningResult = client.propose(prompt);
            if (planningResult == null) {
                log.warn("Spring AI planner returned no planning result; using deterministic fallback");
                return fallback.plan(prompt);
            }

            AiToolProposal proposal = planningResult.proposal();
            if (!toolCatalog.isAllowed(proposal.toolName())) {
                log.warn("Spring AI planner proposed unsupported tool={}; using deterministic fallback",
                        proposal.toolName());
                return fallback.plan(prompt);
            }
            return new AgentPlan(
                    proposal.toolName(),
                    proposal.explanation(),
                    PlannerSource.SPRING_AI,
                    planningResult.usage());
        } catch (RuntimeException ex) {
            log.warn("Spring AI planner failed safely; using deterministic fallback: {}",
                    ex.getMessage());
            return fallback.plan(prompt);
        }
    }
}
