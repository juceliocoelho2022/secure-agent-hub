package br.com.jucelio.secureagent.tool;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedAgentPlanner implements AgentPlanner {
    @Override
    public AgentPlan plan(String prompt) {
        String normalized = prompt.toLowerCase();
        if (normalized.contains("bloque") || normalized.contains("fraude")) {
            return new AgentPlan(
                    "blockCard",
                    "Potential fraud detected; proposes card block",
                    PlannerSource.RULE_BASED);
        }
        if (normalized.contains("transação") || normalized.contains("transacao")) {
            return new AgentPlan(
                    "getTransaction",
                    "Retrieves transaction data for analysis",
                    PlannerSource.RULE_BASED);
        }
        return new AgentPlan(
                "calculateRisk",
                "Calculates a baseline risk score",
                PlannerSource.RULE_BASED);
    }
}
