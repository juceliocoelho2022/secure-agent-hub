package br.com.jucelio.secureagent.audit;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/executions/{executionId}")
    public List<AuditEvent> byExecution(@PathVariable UUID executionId) {
        return service.byExecution(executionId);
    }
}
