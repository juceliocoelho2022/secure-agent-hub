package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        int schemaVersion,
        JsonNode payload
) {
}
