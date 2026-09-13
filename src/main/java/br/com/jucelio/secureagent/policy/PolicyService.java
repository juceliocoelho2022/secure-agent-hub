package br.com.jucelio.secureagent.policy;

import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    public PolicyDecision evaluate(String toolName) {
        return switch (toolName) {
            case "getTransaction", "getCustomer", "calculateRisk" -> PolicyDecision.allow();
            case "blockCard" -> PolicyDecision.requireHuman("Critical operation requires human approval");
            case "deleteAccount" -> PolicyDecision.deny("AI agents cannot delete accounts");
            default -> PolicyDecision.deny("Tool is not registered in policy");
        };
    }
}
