package br.com.jucelio.secureagent.execution;

import java.time.Instant;

public record OperationalTimelineItem(
        String actor,
        String action,
        Instant createdAt,
        String details
) {
}
