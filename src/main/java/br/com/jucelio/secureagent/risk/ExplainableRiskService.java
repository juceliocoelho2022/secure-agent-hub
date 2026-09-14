package br.com.jucelio.secureagent.risk;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExplainableRiskService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");

    public RiskAssessment assess(RiskAssessmentRequest request) {
        int score = 0;
        List<RiskReason> reasons = new ArrayList<>();

        if (request.amount() != null && request.amount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score += 35;
            reasons.add(RiskReason.HIGH_AMOUNT);
        }

        if (hasText(request.country()) && hasText(request.usualCountry())
                && !request.country().equalsIgnoreCase(request.usualCountry())) {
            score += 25;
            reasons.add(RiskReason.FOREIGN_COUNTRY);
        }

        if (request.hour() != null && (request.hour() < 6 || request.hour() >= 23)) {
            score += 20;
            reasons.add(RiskReason.UNUSUAL_HOUR);
        }

        if (request.rapidRetry()) {
            score += 15;
            reasons.add(RiskReason.RAPID_RETRY);
        }

        if (request.knownDevice()) {
            score -= 10;
            reasons.add(RiskReason.KNOWN_DEVICE);
        }

        int normalizedScore = Math.max(0, Math.min(100, score));
        return new RiskAssessment(normalizedScore, RiskLevel.fromScore(normalizedScore), reasons);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
