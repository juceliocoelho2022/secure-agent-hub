package br.com.jucelio.secureagent.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 1000)
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.attempts = 0;
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? "Unknown publish failure" : error.substring(0, Math.min(error.length(), 1000));
    }
}
