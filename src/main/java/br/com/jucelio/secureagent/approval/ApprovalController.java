package br.com.jucelio.secureagent.approval;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<ApprovalRequest> pending() {
        return service.pending();
    }

    @PostMapping("/{id}/approve")
    public ApprovalRequest approve(@PathVariable UUID id, Authentication auth) {
        return service.approve(id, auth.getName());
    }

    @PostMapping("/{id}/reject")
    public ApprovalRequest reject(@PathVariable UUID id, Authentication auth) {
        return service.reject(id, auth.getName());
    }
}
