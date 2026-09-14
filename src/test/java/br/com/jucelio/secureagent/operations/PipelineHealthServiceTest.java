package br.com.jucelio.secureagent.operations;

import br.com.jucelio.secureagent.event.OutboxEventRepository;
import br.com.jucelio.secureagent.event.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineHealthServiceTest {

    @Test
    void snapshotUsesPersistedCountsAndDoesNotInventUnavailableKafkaOrDltMetrics() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        when(outbox.countByPublishedAtIsNull()).thenReturn(3L);
        when(processed.count()).thenReturn(27L);

        PipelineHealthResponse response = new PipelineHealthService(outbox, processed).snapshot();

        assertEquals(3L, response.pendingOutbox());
        assertEquals(27L, response.processedEvents());
        assertEquals("BACKLOG", response.outboxStatus());
        assertEquals("ACTIVE", response.consumerStatus());
        assertEquals("n/a", response.kafkaStatus());
        assertEquals("n/a", response.dltStatus());
        assertNull(response.deadLetterEvents());
    }
}
