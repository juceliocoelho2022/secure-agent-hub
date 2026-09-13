package br.com.jucelio.secureagent.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Column(nullable = false, updatable = false)
    protected UUID id;

    @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
