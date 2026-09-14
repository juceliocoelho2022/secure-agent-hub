package br.com.jucelio.secureagent.risk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRecommendationServiceTest {

    private final RiskRecommendationService service = new RiskRecommendationService();

    @Test
    void criticalRiskRecommendsProtectedCardBlock() {
        RiskAssessment assessment = new RiskAssessment(
                95,
                RiskLevel.CRITICAL,
                List.of(
                        RiskReason.HIGH_AMOUNT,
                        RiskReason.FOREIGN_COUNTRY,
                        RiskReason.UNUSUAL_HOUR,
                        RiskReason.RAPID_RETRY));

        RiskRecommendation recommendation = service.recommend(assessment).orElseThrow();

        assertThat(recommendation.action()).isEqualTo("blockCard");
        assertThat(recommendation.reason()).isEqualTo("CRITICAL_RISK");
    }

    @Test
    void highRiskDoesNotRecommendCriticalAction() {
        RiskAssessment assessment = new RiskAssessment(
                70,
                RiskLevel.HIGH,
                List.of(RiskReason.HIGH_AMOUNT, RiskReason.FOREIGN_COUNTRY));

        assertThat(service.recommend(assessment)).isEmpty();
    }
}
