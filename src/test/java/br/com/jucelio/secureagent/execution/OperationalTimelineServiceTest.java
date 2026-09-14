package br.com.jucelio.secureagent.execution;

import br.com.jucelio.secureagent.audit.AuditEvent;
import br.com.jucelio.secureagent.audit.AuditService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalTimelineServiceTest {

    @Test
    void shouldExposeSanitizedOperationalTimelineWithoutAuditDetails() {
        AuditService auditService = mock(AuditService.class);
        UUID executionId = UUID.randomUUID();
        when(auditService.byExecution(executionId)).thenReturn(List.of(
                new AuditEvent(executionId, "analyst", "EXECUTION_CREATED", "Agent=fraud-agent"),
                new AuditEvent(executionId, "POLICY_ENGINE", "HUMAN_APPROVAL_REQUIRED", "Card blocking requires human approval")
        ));

        OperationalTimelineService service = new OperationalTimelineService(auditService);

        List<OperationalTimelineItem> timeline = service.byExecution(executionId);

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).action()).isEqualTo("EXECUTION_CREATED");
        assertThat(timeline.get(0).actor()).isEqualTo("analyst");
        assertThat(timeline.get(0).details()).isNull();
        assertThat(timeline.get(1).action()).isEqualTo("HUMAN_APPROVAL_REQUIRED");
        assertThat(timeline.get(1).details()).isNull();
    }
}
