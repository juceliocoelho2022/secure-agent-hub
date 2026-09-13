# Spring AI Controlled Planner

## Objective

SecureAgent Hub uses Spring AI as an **intent interpretation layer**, not as an execution authority.

The design rule is:

> The LLM interprets. The Policy Engine authorizes. The backend executes.

## Execution boundary

```text
User prompt
    ↓
AgentPlanner
    ↓
SpringAiAgentPlanner
    ↓
SpringAiPlanningClient / ChatClient
    ↓
AiToolProposal
    ↓
ToolCatalog allowlist
    ↓
AgentPlan(source=SPRING_AI)
    ↓
PolicyService
   ↙      ↓       ↘
DENY    ALLOW   REQUIRE_APPROVAL
          ↓          ↓
      ToolExecutor   HITL
                      ↓
                 ToolExecutor
```

`SpringAiPlanningClient` only requests structured output. It does not register tool callbacks, tool objects, or backend executors with `ChatClient`.

## Planner modes

### Deterministic default

```text
AGENT_PLANNER=rules
```

Uses `RuleBasedAgentPlanner`. This mode is the default and requires no AI provider or API key.

PowerShell:

```powershell
Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
$env:AGENT_PLANNER="rules"
$env:SPRING_AI_CHAT_MODEL="none"
mvn spring-boot:run
```

### Spring AI mode

```text
AGENT_PLANNER=spring-ai
```

Example local activation:

```powershell
$env:AGENT_PLANNER="spring-ai"
$env:SPRING_AI_CHAT_MODEL="openai"
$env:OPENAI_API_KEY="<set-locally-not-in-git>"
$env:OPENAI_CHAT_MODEL="gpt-5-mini"
mvn spring-boot:run
```

Never commit provider API keys.

## Structured output

The AI client is constrained to a proposal shaped as:

```json
{
  "toolName": "blockCard",
  "explanation": "Potential fraud detected"
}
```

The initial integration intentionally does not allow arbitrary execution commands or unconstrained tool arguments.

## Governed tool catalog

Current backend-known tools are:

- `getTransaction`
- `getCustomer`
- `calculateRisk`
- `blockCard`
- `deleteAccount`

A model proposal outside this catalog is not forwarded to execution. The planner falls back to the deterministic rule-based planner.

`deleteAccount` may exist in the catalog so that the Policy Engine can explicitly deny it. Being known to the catalog does **not** mean a tool is authorized.

## Fail-safe behavior

`SpringAiAgentPlanner` falls back to deterministic planning when:

- the provider throws a runtime failure;
- the provider returns no proposal;
- the model proposes an unsupported tool.

Fallback does not bypass policy evaluation. The resulting `AgentPlan` still goes through `PolicyService`.

## Human-in-the-Loop invariant

`blockCard` is a protected operation.

Even when the planner returns:

```text
source = SPRING_AI
tool = blockCard
```

the expected state before human approval remains:

```text
status = WAITING_APPROVAL
requestedTool = blockCard
```

`ToolExecutor` is not called before an authorized operator/admin approves the request.

## Automatic tool calling

Automatic Spring AI tool calling is explicitly disabled:

```yaml
spring:
  ai:
    chat:
      client:
        tool-calling:
          enabled: false
```

This is a defense-in-depth control. The application architecture also avoids supplying tool callbacks or executors to the planning `ChatClient`.

## Auditability

`AgentPlan` carries `PlannerSource`:

- `RULE_BASED`
- `SPRING_AI`

The source is recorded with `TOOL_PLANNED` audit/domain-event metadata so investigations can distinguish deterministic planning from LLM-origin planning without logging provider credentials.

## Threat model notes

Treat all model output as untrusted. Prompt injection may attempt to:

- invent a new tool;
- override system instructions;
- bypass human approval;
- claim a tool already executed;
- request secrets or privileged data.

The primary controls are backend allowlisting, Policy Engine enforcement, RBAC, Human-in-the-Loop, auditability and the absence of direct LLM execution authority.

## CI strategy

CI must never require a live provider. Unit tests mock the Spring AI boundary and verify:

- structured proposal conversion;
- unsupported-tool fallback;
- provider-failure fallback;
- rule-based default selection;
- Spring AI planner selection;
- automatic tool calling disabled;
- `blockCard` from `SPRING_AI` still requires Human-in-the-Loop.

Full verification:

```bash
mvn -B -ntp clean verify
```
