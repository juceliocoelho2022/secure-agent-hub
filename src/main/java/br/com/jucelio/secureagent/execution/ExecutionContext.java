package br.com.jucelio.secureagent.execution;

import java.math.BigDecimal;

public record ExecutionContext(
        String transactionId,
        BigDecimal amount,
        String country,
        String usualCountry,
        Integer hour,
        boolean rapidRetry,
        boolean knownDevice
) {}
