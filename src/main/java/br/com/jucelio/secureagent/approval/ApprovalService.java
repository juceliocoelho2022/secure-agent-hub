package br.com.jucelio.secureagent.approval;

import br.com.jucelio.secureagent.audit.AuditService;
import br.com.jucelio.secureagent.execution.AgentExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApprovalService {
    private final ApprovalRequestRepository repository;
    private final AgentExecutionService executionService;
    private final AuditService auditService;

    public ApprovalService(ApprovalRequestRepository repository,
                           AgentExecutionService executionService,
                           AuditService auditService) {
        this.repository = repository;
        this.executionService = executionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> pending() {
        return repository.findAllByStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING);
    }

    @Transactional
    public ApprovalRequest approve(UUID id, String username) {
        ApprovalRequest request = get(id);
        request.approve(username);
        repository.save(request);
        auditService.record(request.getExecutionId(), username, "HUMAN_APPROVED", request.getToolName());
        executionService.executeApprovedTool(request.getExecutionId(), request.getToolName(), username);
        return request;
    }

    @Transactional
    public ApprovalRequest reject(UUID id, String username) {
        ApprovalRequest request = get(id);
        request.reject(username);
        repository.save(request);
        auditService.record(request.getExecutionId(), username, "HUMAN_REJECTED", request.getToolName());
        executionService.markRejected(request.getExecutionId(), username);
        return request;
    }

    private ApprovalRequest get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Approval request not found"));
    }
}
