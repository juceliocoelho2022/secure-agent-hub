package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class DomainEventService {

    private static final String AGGREGATE_TYPE = "AgentExecution";
    private static final int SCHEMA_VERSION = 1;

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public DomainEventService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void append(UUID aggregateId, String eventType, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        JsonNode payloadNode = objectMapper.valueToTree(payload);

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                eventType,
                AGGREGATE_TYPE,
                aggregateId,
                occurredAt,
                aggregateId.toString(),
                null,
                SCHEMA_VERSION,
                payloadNode
        );

        try {
            String json = objectMapper.writeValueAsString(envelope);
            repository.save(new OutboxEvent(
                    eventId,
                    AGGREGATE_TYPE,
                    aggregateId,
                    eventType,
                    json,
                    occurredAt
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize domain event envelope", e);
        }
    }
}
