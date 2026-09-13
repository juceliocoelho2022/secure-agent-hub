package br.com.jucelio.secureagent.approval;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID executionId;
    @Column(nullable = false, length = 120)
    private String toolName;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant decidedAt;
    @Column(length = 120)
    private String decidedBy;

    protected ApprovalRequest() {}

    public ApprovalRequest(UUID executionId, String toolName, String reason) {
        this.id = UUID.randomUUID();
        this.executionId = executionId;
        this.toolName = toolName;
        this.reason = reason;
        this.status = ApprovalStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public String getToolName() { return toolName; }
    public String getReason() { return reason; }
    public ApprovalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecidedBy() { return decidedBy; }

    public void approve(String username) {
        ensurePending();
        this.status = ApprovalStatus.APPROVED;
        this.decidedBy = username;
        this.decidedAt = Instant.now();
    }

    public void reject(String username) {
        ensurePending();
        this.status = ApprovalStatus.REJECTED;
        this.decidedBy = username;
        this.decidedAt = Instant.now();
    }

    private void ensurePending() {
        if (status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request already decided");
        }
    }
}
