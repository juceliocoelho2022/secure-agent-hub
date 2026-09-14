package br.com.jucelio.secureagent.risk;

import java.util.List;

public record RiskAssessment(
        int score,
        RiskLevel level,
        List<RiskReason> reasons
) {
    public RiskAssessment {
        reasons = List.copyOf(reasons);
    }
}
