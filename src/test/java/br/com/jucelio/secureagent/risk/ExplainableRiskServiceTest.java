package br.com.jucelio.secureagent.risk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainableRiskServiceTest {

    private final ExplainableRiskService service = new ExplainableRiskService();

    @Test
    void shouldProduceCriticalExplainableAssessmentFromStructuredSignals() {
        RiskAssessmentRequest request = new RiskAssessmentRequest(
                "TX-9001",
                new BigDecimal("9800.00"),
                "US",
                "BR",
                2,
                true,
                false);

        RiskAssessment assessment = service.assess(request);

        assertThat(assessment.score()).isEqualTo(95);
        assertThat(assessment.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assessment.reasons()).containsExactly(
                RiskReason.HIGH_AMOUNT,
                RiskReason.FOREIGN_COUNTRY,
                RiskReason.UNUSUAL_HOUR,
                RiskReason.RAPID_RETRY);
    }

    @Test
    void shouldReduceRiskForKnownDeviceAndNeverGoBelowZero() {
        RiskAssessmentRequest request = new RiskAssessmentRequest(
                "TX-LOW",
                new BigDecimal("100.00"),
                "BR",
                "BR",
                14,
                false,
                true);

        RiskAssessment assessment = service.assess(request);

        assertThat(assessment.score()).isZero();
        assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasons()).containsExactly(RiskReason.KNOWN_DEVICE);
    }

    @Test
    void shouldMapRiskLevelBoundariesDeterministically() {
        assertThat(RiskLevel.fromScore(0)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromScore(29)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromScore(30)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(59)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(60)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(79)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(80)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(RiskLevel.fromScore(100)).isEqualTo(RiskLevel.CRITICAL);
    }
}
