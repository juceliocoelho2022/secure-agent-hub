package br.com.jucelio.secureagent.execution;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/executions")
public class AgentExecutionController {
    private final AgentExecutionService service;
    private final OperationalTimelineService timelineService;

    public AgentExecutionController(AgentExecutionService service,
                                    OperationalTimelineService timelineService) {
        this.service = service;
        this.timelineService = timelineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionResponse create(@Valid @RequestBody CreateExecutionRequest request, Authentication auth) {
        return ExecutionResponse.from(service.create(request, auth.getName()));
    }

    @GetMapping("/{id}")
    public ExecutionResponse get(@PathVariable UUID id) {
        return ExecutionResponse.from(service.get(id));
    }

    @GetMapping("/{id}/timeline")
    public List<OperationalTimelineItem> timeline(@PathVariable UUID id) {
        return timelineService.byExecution(id);
    }

    @GetMapping
    public List<ExecutionResponse> list() {
        return service.list().stream().map(ExecutionResponse::from).toList();
    }
}
