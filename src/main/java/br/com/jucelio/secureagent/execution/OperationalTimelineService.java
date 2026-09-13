package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OperationalTimelineService {
    private final AuditService auditService;

    public OperationalTimelineService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OperationalTimelineItem> byExecution(UUID executionId) {
        return auditService.byExecution(executionId).stream()
                .map(event -> new OperationalTimelineItem(
                        event.getActor(),
                        event.getAction(),
                        event.getCreatedAt(),
                        null))
                .toList();
    }
}
