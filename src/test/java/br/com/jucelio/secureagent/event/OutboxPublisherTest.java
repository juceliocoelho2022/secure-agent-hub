package br.com.jucelio.secureagent.event;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    @Test
    void shouldMarkEventPublishedWhenKafkaSendSucceeds() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        OutboxEvent event = event();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("secure-agent.events", event.getAggregateId().toString(), event.getPayload()))
                .thenReturn(future);

        new OutboxPublisher(repository, kafkaTemplate, "secure-agent.events").publishPending();

        assertNotNull(event.getPublishedAt());
        assertEquals(0, event.getAttempts());
        assertNull(event.getLastError());
    }

    @Test
    void shouldRecordFailureWhenKafkaSendFails() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        OutboxEvent event = event();
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send("secure-agent.events", event.getAggregateId().toString(), event.getPayload()))
                .thenReturn(future);

        new OutboxPublisher(repository, kafkaTemplate, "secure-agent.events").publishPending();

        assertNull(event.getPublishedAt());
        assertEquals(1, event.getAttempts());
        assertNotNull(event.getLastError());
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                "AgentExecution",
                UUID.randomUUID(),
                "agent.execution.created",
                "{\"eventType\":\"agent.execution.created\"}"
        );
    }
}
