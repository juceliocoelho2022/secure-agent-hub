package br.com.jucelio.secureagent.risk;

import java.math.BigDecimal;

public record RiskAssessmentRequest(
        String transactionId,
        BigDecimal amount,
        String country,
        String usualCountry,
        Integer hour,
        boolean rapidRetry,
        boolean knownDevice
) {}
