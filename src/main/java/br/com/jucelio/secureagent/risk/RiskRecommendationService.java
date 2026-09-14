package br.com.jucelio.secureagent.risk;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RiskRecommendationService {

    public Optional<RiskRecommendation> recommend(RiskAssessment assessment) {
        if (assessment.level() == RiskLevel.CRITICAL) {
            return Optional.of(new RiskRecommendation("blockCard", "CRITICAL_RISK"));
        }
        return Optional.empty();
    }
}
