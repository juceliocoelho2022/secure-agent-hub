# Spring AI Controlled Planner Design

## Objective

Introduce Spring AI into SecureAgent Hub as an intent interpreter while preserving the existing security boundary: the LLM may propose an action, but only the backend Policy Engine and Human-in-the-Loop flow may authorize execution.

## Architectural principle

> The LLM interprets. The Policy Engine authorizes. The backend executes.

The model must never receive direct authority to execute protected tools.

## Current boundary

`AgentExecutionService` depends on the `AgentPlanner` interface and currently receives an `AgentPlan(toolName, explanation)`. The service then evaluates the tool through `PolicyService`, creates approvals when required, and only invokes `ToolExecutor` after policy/HITL checks. This boundary is retained.

## Planner modes

Two planner implementations will coexist:

- `RuleBasedAgentPlanner`: deterministic local/default fallback.
- `SpringAiAgentPlanner`: AI-backed structured intent interpretation.

Planner selection is configuration-driven. Local development and CI must continue to work without an OpenAI API key.

## Spring AI behavior

`SpringAiAgentPlanner` uses `ChatClient` only to produce a structured proposal. Automatic Spring AI tool execution is disabled. The model output is converted into a strict DTO containing a tool name and explanation.

Allowed tool names are constrained to the backend tool catalog. Unsupported or malformed AI proposals fail closed and fall back to the deterministic planner rather than being executed.

## Execution flow

```text
User prompt
  -> AgentPlanner
      -> RuleBasedAgentPlanner OR SpringAiAgentPlanner
  -> AgentPlan
  -> PolicyService
      -> DENY
      -> ALLOW
      -> REQUIRE_APPROVAL
  -> Human Approval when required
  -> ToolExecutor
  -> Audit + Domain Events
```

The Spring AI layer cannot bypass `PolicyService`, approval persistence, or `ToolExecutor`.

## Configuration

Existing Spring AI models remain disabled by default. Add application-level planner selection, for example:

```yaml
app:
  agent:
    planner: ${AGENT_PLANNER:rules}
```

When `AGENT_PLANNER=spring-ai`, the application requires an enabled chat model and API key. Otherwise the rule-based planner remains active.

Spring AI automatic tool calling is explicitly disabled:

```yaml
spring:
  ai:
    chat:
      client:
        tool-calling:
          enabled: false
```

## Structured output

The AI planner returns a constrained proposal such as:

```json
{
  "toolName": "blockCard",
  "explanation": "Potential fraud detected and card block proposed"
}
```

The initial v1.3 integration does not permit arbitrary tool arguments or free-form execution commands. Argument-aware tools can be introduced later behind a validated schema.

## Auditability

Planning provenance must be observable. `TOOL_PLANNED` audit/event metadata should identify whether the plan originated from `RULE_BASED` or `SPRING_AI` without exposing secrets or raw API credentials.

## Failure behavior

AI failures are fail-safe:

- malformed structured output -> deterministic fallback;
- unavailable provider -> deterministic fallback;
- unsupported tool -> deterministic fallback;
- no API key in default mode -> application still starts with rule-based planning.

No provider failure may trigger tool execution by itself.

## Testing

CI must not call external AI providers. Unit tests use mocked/stubbed Spring AI responses and verify:

- AI output becomes an `AgentPlan`;
- unsupported tools are rejected/fallback;
- provider errors fallback safely;
- Policy Engine remains downstream of the planner;
- protected `blockCard` still reaches `WAITING_APPROVAL` rather than direct execution;
- automatic tool calling remains disabled by configuration.

## Out of scope for this increment

- RAG/pgvector;
- MCP;
- autonomous multi-step tool loops;
- direct LLM tool execution;
- production token cost dashboards;
- provider failover across multiple vendors.
