package br.com.jucelio.secureagent.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID executionId;
    @Column(nullable = false, length = 80)
    private String actor;
    @Column(nullable = false, length = 120)
    private String action;
    @Column(nullable = false, length = 4000)
    private String details;
    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEvent() {}

    public AuditEvent(UUID executionId, String actor, String action, String details) {
        this.id = UUID.randomUUID();
        this.executionId = executionId;
        this.actor = actor;
        this.action = action;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
