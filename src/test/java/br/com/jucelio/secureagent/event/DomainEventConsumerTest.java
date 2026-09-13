package br.com.jucelio.secureagent.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldPersistSuccessfullyProcessedEvent() throws Exception {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        DomainEventConsumer consumer = new DomainEventConsumer(objectMapper, repository, "agent.test.failure");
        DomainEventEnvelope envelope = envelope("agent.execution.created");
        when(repository.existsById(envelope.eventId())).thenReturn(false);

        consumer.consume(objectMapper.writeValueAsString(envelope));

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(repository).save(captor.capture());
        assertEquals(envelope.eventId(), captor.getValue().getEventId());
        assertEquals("agent.execution.created", captor.getValue().getEventType());
    }

    @Test
    void shouldIgnoreDuplicateEvent() throws Exception {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        DomainEventConsumer consumer = new DomainEventConsumer(objectMapper, repository, "agent.test.failure");
        DomainEventEnvelope envelope = envelope("agent.execution.created");
        when(repository.existsById(envelope.eventId())).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(envelope));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailControlledEventWithoutMarkingItProcessed() throws Exception {
        ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
        DomainEventConsumer consumer = new DomainEventConsumer(objectMapper, repository, "agent.test.failure");
        DomainEventEnvelope envelope = envelope("agent.test.failure");
        when(repository.existsById(envelope.eventId())).thenReturn(false);
        String json = objectMapper.writeValueAsString(envelope);

        assertThrows(IllegalStateException.class, () -> consumer.consume(json));
        verify(repository, never()).save(any());
    }

    private DomainEventEnvelope envelope(String eventType) {
        UUID aggregateId = UUID.randomUUID();
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                "AgentExecution",
                aggregateId,
                Instant.now(),
                aggregateId.toString(),
                null,
                1,
                objectMapper.valueToTree(java.util.Map.of("test", true))
        );
    }
}
