package br.com.jucelio.secureagent.execution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExecutionRequest(
        @NotBlank @Size(max = 80) String agent,
        @NotBlank @Size(max = 2000) String prompt,
        ExecutionContext context
) {}
