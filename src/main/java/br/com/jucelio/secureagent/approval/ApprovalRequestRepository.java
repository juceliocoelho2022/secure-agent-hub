package br.com.jucelio.secureagent.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    List<ApprovalRequest> findAllByStatusOrderByCreatedAtAsc(ApprovalStatus status);
}
