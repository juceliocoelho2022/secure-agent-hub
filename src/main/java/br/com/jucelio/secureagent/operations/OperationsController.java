package br.com.jucelio.secureagent.operations;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationsController {
    private final PipelineHealthService pipelineHealthService;

    public OperationsController(PipelineHealthService pipelineHealthService) {
        this.pipelineHealthService = pipelineHealthService;
    }

    @GetMapping("/pipeline-health")
    public PipelineHealthResponse pipelineHealth() {
        return pipelineHealthService.snapshot();
    }
}
