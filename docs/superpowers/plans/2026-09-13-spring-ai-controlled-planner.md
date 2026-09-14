# Spring AI Controlled Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Spring AI-backed planner that converts user prompts into validated tool proposals while preserving Policy Engine, Human-in-the-Loop, audit, and backend-controlled execution as the only authorization path.

**Architecture:** Keep `AgentExecutionService -> AgentPlanner -> AgentPlan -> PolicyService -> HITL/ToolExecutor` intact. Add `SpringAiAgentPlanner` as an alternate implementation selected by configuration, with `RuleBasedAgentPlanner` remaining the default and fallback. The Spring AI layer must only produce structured planning output; automatic tool execution remains disabled.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring AI 1.1.8, Spring AI `ChatClient`, JUnit 5, Mockito, Spring Security, PostgreSQL, Kafka, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-13-spring-ai-controlled-planner-design.md`

## Global Constraints

- Preserve the principle: **The LLM interprets. The Policy Engine authorizes. The backend executes.**
- Default local and CI startup must work with no OpenAI API key.
- `RuleBasedAgentPlanner` remains available and is the default mode.
- `SpringAiAgentPlanner` may propose tools but must never invoke `ToolExecutor`.
- Spring AI automatic tool calling must remain disabled.
- Unsupported, malformed, or failed AI planning must fail closed to deterministic fallback.
- CI must not call external AI providers.
- `blockCard` must still result in `WAITING_APPROVAL` before any tool execution.
- Do not introduce RAG, MCP, autonomous tool loops, arbitrary tool arguments, or provider failover in this increment.

---

### Task 1: Introduce planning provenance without changing authorization flow

**Files:**
- Modify: `src/main/java/br/com/jucelio/secureagent/tool/AgentPlan.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/tool/RuleBasedAgentPlanner.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/AgentExecutionService.java`
- Test: `src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java`

**Interfaces:**
- Consumes: existing `AgentPlanner#plan(String prompt)`.
- Produces: `AgentPlan(String toolName, String explanation, PlannerSource source)` and enum `PlannerSource { RULE_BASED, SPRING_AI }`.

- [ ] **Step 1: Write the failing test**

Add assertions to `AgentExecutionServiceTest` verifying that the planned audit/event metadata records `RULE_BASED` provenance for the current planner.

Example expectation:

```java
verify(auditService).record(
        eq(executionId),
        eq("AI_AGENT"),
        eq("TOOL_PLANNED"),
        contains("source=RULE_BASED"));
```

- [ ] **Step 2: Run the targeted test and verify RED**

Run:

```bash
mvn -B -ntp -Dtest=AgentExecutionServiceTest test
```

Expected: FAIL because `AgentPlan` has no source metadata yet.

- [ ] **Step 3: Implement minimal provenance support**

Create:

```java
package br.com.jucelio.secureagent.tool;

public enum PlannerSource {
    RULE_BASED,
    SPRING_AI
}
```

Change `AgentPlan` to:

```java
public record AgentPlan(
        String toolName,
        String explanation,
        PlannerSource source
) {}
```

Update `RuleBasedAgentPlanner` to construct plans with `PlannerSource.RULE_BASED`.

Update `AgentExecutionService` audit/event details to include `plan.source().name()` while keeping `PolicyService` directly downstream of planning.

- [ ] **Step 4: Run targeted tests and verify GREEN**

```bash
mvn -B -ntp -Dtest=AgentExecutionServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/jucelio/secureagent/tool src/main/java/br/com/jucelio/secureagent/execution/AgentExecutionService.java src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java
git commit -m "feat: track agent planner provenance"
```

---

### Task 2: Add a strict backend tool catalog for AI proposal validation

**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/tool/ToolCatalog.java`
- Test: `src/test/java/br/com/jucelio/secureagent/tool/ToolCatalogTest.java`

**Interfaces:**
- Consumes: tool names already understood by `PolicyService` / `ToolExecutor`.
- Produces: `boolean isAllowed(String toolName)` and `Set<String> allowedToolNames()`.

- [ ] **Step 1: Write the failing test**

```java
class ToolCatalogTest {
    private final ToolCatalog catalog = new ToolCatalog();

    @Test
    void acceptsOnlyBackendKnownTools() {
        assertTrue(catalog.isAllowed("getTransaction"));
        assertTrue(catalog.isAllowed("getCustomer"));
        assertTrue(catalog.isAllowed("calculateRisk"));
        assertTrue(catalog.isAllowed("blockCard"));
        assertTrue(catalog.isAllowed("deleteAccount"));
        assertFalse(catalog.isAllowed("runShellCommand"));
    }
}
```

- [ ] **Step 2: Run targeted test and verify RED**

```bash
mvn -B -ntp -Dtest=ToolCatalogTest test
```

Expected: FAIL because `ToolCatalog` does not exist.

- [ ] **Step 3: Implement the catalog**

Use an immutable set:

```java
private static final Set<String> ALLOWED = Set.of(
        "getTransaction",
        "getCustomer",
        "calculateRisk",
        "blockCard",
        "deleteAccount"
);
```

Expose `isAllowed` and `allowedToolNames` without executing any tool.

- [ ] **Step 4: Run targeted test and verify GREEN**

```bash
mvn -B -ntp -Dtest=ToolCatalogTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/jucelio/secureagent/tool/ToolCatalog.java src/test/java/br/com/jucelio/secureagent/tool/ToolCatalogTest.java
git commit -m "feat: add governed backend tool catalog"
```

---

### Task 3: Add structured AI proposal model and adapter boundary

**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/ai/AiToolProposal.java`
- Create: `src/main/java/br/com/jucelio/secureagent/ai/AiPlanningClient.java`
- Test: `src/test/java/br/com/jucelio/secureagent/ai/AiToolProposalTest.java`

**Interfaces:**
- Produces: `record AiToolProposal(String toolName, String explanation)`.
- Produces: `AiToolProposal propose(String prompt)` in `AiPlanningClient`.
- Later consumed by `SpringAiAgentPlanner`.

- [ ] **Step 1: Write the failing validation test**

```java
@Test
void proposalRequiresToolNameAndExplanation() {
    assertThrows(IllegalArgumentException.class,
            () -> new AiToolProposal("", "reason"));
    assertThrows(IllegalArgumentException.class,
            () -> new AiToolProposal("blockCard", ""));
}
```

- [ ] **Step 2: Run targeted test and verify RED**

```bash
mvn -B -ntp -Dtest=AiToolProposalTest test
```

Expected: FAIL because the type does not exist.

- [ ] **Step 3: Implement the record and interface**

```java
public record AiToolProposal(String toolName, String explanation) {
    public AiToolProposal {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName is required");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation is required");
        }
    }
}
```

```java
public interface AiPlanningClient {
    AiToolProposal propose(String prompt);
}
```

- [ ] **Step 4: Run targeted test and verify GREEN**

```bash
mvn -B -ntp -Dtest=AiToolProposalTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/jucelio/secureagent/ai src/test/java/br/com/jucelio/secureagent/ai
git commit -m "feat: define structured AI planning proposal"
```

---

### Task 4: Implement the fail-safe Spring AI planner with deterministic fallback

**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/tool/SpringAiAgentPlanner.java`
- Test: `src/test/java/br/com/jucelio/secureagent/tool/SpringAiAgentPlannerTest.java`

**Interfaces:**
- Consumes: `AiPlanningClient`, `ToolCatalog`, and fallback `RuleBasedAgentPlanner` through `AgentPlanner`.
- Produces: `AgentPlan` with `PlannerSource.SPRING_AI` when valid; fallback plan otherwise.

- [ ] **Step 1: Write failing tests for valid, unsupported, and provider-failure paths**

Tests must verify:

```java
@Test
void returnsSpringAiPlanForAllowedStructuredProposal() {
    when(client.propose("fraud prompt"))
            .thenReturn(new AiToolProposal("blockCard", "fraud detected"));

    AgentPlan result = planner.plan("fraud prompt");

    assertEquals("blockCard", result.toolName());
    assertEquals(PlannerSource.SPRING_AI, result.source());
    verify(fallback, never()).plan(anyString());
}
```

```java
@Test
void fallsBackWhenAiSuggestsUnsupportedTool() {
    when(client.propose(anyString()))
            .thenReturn(new AiToolProposal("runShellCommand", "do it"));
    when(fallback.plan(anyString()))
            .thenReturn(new AgentPlan("calculateRisk", "fallback", PlannerSource.RULE_BASED));

    AgentPlan result = planner.plan("prompt");

    assertEquals(PlannerSource.RULE_BASED, result.source());
    verify(fallback).plan("prompt");
}
```

```java
@Test
void fallsBackWhenProviderFails() {
    when(client.propose(anyString())).thenThrow(new RuntimeException("provider unavailable"));
    when(fallback.plan(anyString()))
            .thenReturn(new AgentPlan("calculateRisk", "fallback", PlannerSource.RULE_BASED));

    AgentPlan result = planner.plan("prompt");

    assertEquals(PlannerSource.RULE_BASED, result.source());
}
```

- [ ] **Step 2: Run targeted tests and verify RED**

```bash
mvn -B -ntp -Dtest=SpringAiAgentPlannerTest test
```

Expected: FAIL because planner class does not exist.

- [ ] **Step 3: Implement minimal planner logic**

Algorithm:

```java
try {
    AiToolProposal proposal = client.propose(prompt);
    if (!toolCatalog.isAllowed(proposal.toolName())) {
        return fallback.plan(prompt);
    }
    return new AgentPlan(
            proposal.toolName(),
            proposal.explanation(),
            PlannerSource.SPRING_AI);
} catch (RuntimeException ex) {
    return fallback.plan(prompt);
}
```

The class must not depend on `PolicyService`, `ApprovalService`, or `ToolExecutor`.

- [ ] **Step 4: Run targeted tests and verify GREEN**

```bash
mvn -B -ntp -Dtest=SpringAiAgentPlannerTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/jucelio/secureagent/tool/SpringAiAgentPlanner.java src/test/java/br/com/jucelio/secureagent/tool/SpringAiAgentPlannerTest.java
git commit -m "feat: add fail-safe Spring AI agent planner"
```

---

### Task 5: Implement the `ChatClient` structured-output adapter without tool execution

**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/ai/SpringAiPlanningClient.java`
- Create: `src/main/java/br/com/jucelio/secureagent/ai/SpringAiPlanningConfiguration.java`
- Test: `src/test/java/br/com/jucelio/secureagent/ai/SpringAiPlanningClientTest.java`

**Interfaces:**
- Consumes: Spring AI `ChatClient.Builder`.
- Implements: `AiPlanningClient#propose(String prompt)`.
- Must return only `AiToolProposal`.

- [ ] **Step 1: Write a test around the adapter boundary**

Use mocked `ChatClient` fluent API or a minimal fake around the builder so no HTTP call occurs. Verify that the client requests structured entity conversion to `AiToolProposal.class` and includes the allowed tool contract in the system prompt.

Expected system constraints include:

```text
You are an intent planner, not an executor.
Return exactly one backend tool proposal.
Never execute tools.
Allowed tools: getTransaction, getCustomer, calculateRisk, blockCard, deleteAccount.
```

- [ ] **Step 2: Run targeted test and verify RED**

```bash
mvn -B -ntp -Dtest=SpringAiPlanningClientTest test
```

Expected: FAIL because adapter/configuration do not exist.

- [ ] **Step 3: Implement `SpringAiPlanningClient`**

Use `ChatClient` only for structured output:

```java
return chatClient.prompt()
        .system(systemPrompt())
        .user(prompt)
        .call()
        .entity(AiToolProposal.class);
```

Do not register tools, functions, callbacks, or executors with the client.

- [ ] **Step 4: Provide the `ChatClient` bean only when Spring AI planner mode is active**

Use configuration conditional on:

```text
app.agent.planner=spring-ai
```

The bean construction must not require an AI provider when planner mode is `rules`.

- [ ] **Step 5: Run targeted test and verify GREEN**

```bash
mvn -B -ntp -Dtest=SpringAiPlanningClientTest test
```

Expected: PASS with no external network activity.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/jucelio/secureagent/ai src/test/java/br/com/jucelio/secureagent/ai/SpringAiPlanningClientTest.java
git commit -m "feat: adapt ChatClient to structured planning output"
```

---

### Task 6: Add configuration-driven planner selection and explicitly disable automatic tool calling

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/br/com/jucelio/secureagent/tool/RuleBasedAgentPlanner.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/tool/SpringAiAgentPlanner.java`
- Test: `src/test/java/br/com/jucelio/secureagent/config/PlannerConfigurationTest.java`

**Interfaces:**
- Consumes: `app.agent.planner` with values `rules` or `spring-ai`.
- Produces: exactly one active `AgentPlanner` bean.

- [ ] **Step 1: Write failing context tests**

Test default mode:

```java
new ApplicationContextRunner()
    .withUserConfiguration(PlannerBeans.class)
    .withPropertyValues("app.agent.planner=rules")
    .run(context -> {
        assertThat(context).hasSingleBean(AgentPlanner.class);
        assertThat(context.getBean(AgentPlanner.class))
                .isInstanceOf(RuleBasedAgentPlanner.class);
    });
```

Test AI mode similarly expects `SpringAiAgentPlanner` with mocked `AiPlanningClient`.

- [ ] **Step 2: Run targeted tests and verify RED**

```bash
mvn -B -ntp -Dtest=PlannerConfigurationTest test
```

Expected: FAIL because planner bean selection is not conditional yet.

- [ ] **Step 3: Implement conditional planner beans**

Use `@ConditionalOnProperty` so:

- `RuleBasedAgentPlanner` is active when `app.agent.planner=rules` or property missing.
- `SpringAiAgentPlanner` is active only when `app.agent.planner=spring-ai`.
- inject a dedicated rule-based fallback into the AI planner without making it the primary `AgentPlanner` bean in AI mode.

- [ ] **Step 4: Update YAML safely**

Add:

```yaml
spring:
  ai:
    chat:
      client:
        tool-calling:
          enabled: false

app:
  agent:
    planner: ${AGENT_PLANNER:rules}
```

Keep all OpenAI credentials environment-driven.

- [ ] **Step 5: Run configuration tests and verify GREEN**

```bash
mvn -B -ntp -Dtest=PlannerConfigurationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.yml src/main/java/br/com/jucelio/secureagent/tool src/test/java/br/com/jucelio/secureagent/config/PlannerConfigurationTest.java
git commit -m "feat: select agent planner by configuration"
```

---

### Task 7: Prove Policy Engine and Human-in-the-Loop remain authoritative with an AI-origin plan

**Files:**
- Modify: `src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java`

**Interfaces:**
- Consumes: `AgentPlanner` returning `AgentPlan("blockCard", ..., SPRING_AI)`.
- Verifies: `PolicyService` is called, approval is persisted, `ToolExecutor` is not called before approval.

- [ ] **Step 1: Write the regression test**

```java
@Test
void springAiBlockCardProposalStillRequiresHumanApproval() {
    when(planner.plan(anyString()))
            .thenReturn(new AgentPlan(
                    "blockCard",
                    "Potential fraud detected",
                    PlannerSource.SPRING_AI));
    when(policyService.evaluate("blockCard"))
            .thenReturn(new PolicyDecision(true, true, "Critical operation requires human approval"));

    AgentExecution result = service.create(
            new CreateExecutionRequest("fraud-agent", "block suspicious card"),
            "operator");

    assertEquals(ExecutionStatus.WAITING_APPROVAL, result.getStatus());
    verify(policyService).evaluate("blockCard");
    verify(approvalRepository).save(any(ApprovalRequest.class));
    verify(toolExecutor, never()).execute(anyString());
}
```

- [ ] **Step 2: Run targeted test and verify behavior**

```bash
mvn -B -ntp -Dtest=AgentExecutionServiceTest test
```

Expected: PASS only if Spring AI provenance does not alter security flow.

- [ ] **Step 3: Refactor only if the test reveals coupling**

Any required production change must preserve this sequence:

```text
AgentPlanner -> PolicyService -> ApprovalRequest or ToolExecutor
```

Never inject `ToolExecutor` into `SpringAiAgentPlanner` or `SpringAiPlanningClient`.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java src/main/java/br/com/jucelio/secureagent
git commit -m "test: enforce HITL for Spring AI protected tools"
```

---

### Task 8: Document activation, fallback, threat boundary, and local validation

**Files:**
- Modify: `README.md`
- Create: `docs/SPRING_AI.md`
- Modify: `SECURITY.md`

**Interfaces:**
- Documents environment variables:
  - `AGENT_PLANNER=rules|spring-ai`
  - `SPRING_AI_CHAT_MODEL=none|openai`
  - `OPENAI_API_KEY`
  - `OPENAI_CHAT_MODEL`

- [ ] **Step 1: Document safe default mode**

Include:

```powershell
$env:AGENT_PLANNER="rules"
mvn spring-boot:run
```

Expected: application starts with no OpenAI API key.

- [ ] **Step 2: Document AI mode**

Include:

```powershell
$env:AGENT_PLANNER="spring-ai"
$env:SPRING_AI_CHAT_MODEL="openai"
$env:OPENAI_API_KEY="<set-locally-not-in-git>"
$env:OPENAI_CHAT_MODEL="gpt-5-mini"
mvn spring-boot:run
```

- [ ] **Step 3: Document the security boundary**

Explicitly state:

```text
ChatClient -> structured proposal only
Policy Engine -> authorization
Human Approval -> critical-action gate
ToolExecutor -> backend-controlled execution
```

Also document prompt injection risk and the backend allowlist/fallback controls.

- [ ] **Step 4: Commit**

```bash
git add README.md SECURITY.md docs/SPRING_AI.md
git commit -m "docs: explain controlled Spring AI planning"
```

---

### Task 9: Full verification and PR readiness checkpoint

**Files:**
- No production files unless verification exposes a defect.

**Interfaces:**
- Validates the whole v1.3 controlled-planner increment.

- [ ] **Step 1: Run the complete Maven verification**

```bash
mvn -B -ntp clean verify
```

Expected: `BUILD SUCCESS`, zero test failures.

- [ ] **Step 2: Verify default no-key startup locally**

```powershell
Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
$env:AGENT_PLANNER="rules"
$env:SPRING_AI_CHAT_MODEL="none"
mvn spring-boot:run
```

Then:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected:

```text
status = UP
```

- [ ] **Step 3: Verify governed runtime path in rule mode**

Authenticate as `operator`, submit:

```text
Analise possível fraude na transação TX-9001 e bloqueie o cartão.
```

Expected before approval:

```text
status = WAITING_APPROVAL
requestedTool = blockCard
```

Then approve and verify final completion through the dashboard timeline.

- [ ] **Step 4: Verify CI on the latest branch head**

Confirm GitHub Actions `Build & Test (Java 21)` completed successfully for the latest commit before calling the increment complete.

- [ ] **Step 5: Update PR #3 checklist/body only after evidence is green**

Mark only implemented items complete; leave RAG, MCP, and production token-cost dashboards out of v1.3.
