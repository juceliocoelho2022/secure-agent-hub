package br.com.jucelio.secureagent.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID executionId, String actor, String action, String details) {
        repository.save(new AuditEvent(executionId, actor, action, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> byExecution(UUID executionId) {
        return repository.findByExecutionIdOrderByCreatedAtAsc(executionId);
    }
}
