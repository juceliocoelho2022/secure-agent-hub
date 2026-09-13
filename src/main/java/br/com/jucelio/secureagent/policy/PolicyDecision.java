package br.com.jucelio.secureagent.policy;

public record PolicyDecision(boolean allowed, boolean humanApprovalRequired, String reason) {
    public static PolicyDecision allow() {
        return new PolicyDecision(true, false, "Allowed by policy");
    }
    public static PolicyDecision requireHuman(String reason) {
        return new PolicyDecision(true, true, reason);
    }
    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(false, false, reason);
    }
}
