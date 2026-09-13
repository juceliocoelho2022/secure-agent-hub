package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DomainEventServiceTest {

    @Test
    void shouldPersistStandardEnvelopeUsingSameEventIdAsOutboxId() throws Exception {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        DomainEventService service = new DomainEventService(repository, objectMapper);
        UUID aggregateId = UUID.randomUUID();

        service.append(aggregateId, "agent.execution.created", Map.of(
                "executionId", aggregateId.toString(),
                "agent", "fraud-agent"
        ));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        DomainEventEnvelope envelope = objectMapper.readValue(saved.getPayload(), DomainEventEnvelope.class);

        assertEquals(saved.getId(), envelope.eventId());
        assertEquals("AgentExecution", envelope.aggregateType());
        assertEquals(aggregateId, envelope.aggregateId());
        assertEquals("agent.execution.created", envelope.eventType());
        assertEquals(aggregateId.toString(), envelope.correlationId());
        assertEquals(1, envelope.schemaVersion());
        assertEquals("fraud-agent", envelope.payload().get("agent").asText());
        assertNotNull(envelope.occurredAt());
    }
}
