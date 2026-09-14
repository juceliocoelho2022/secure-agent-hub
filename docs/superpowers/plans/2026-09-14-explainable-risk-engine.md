# Explainable Risk Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the mock risk calculation with a deterministic, explainable, persisted risk assessment driven by structured execution context and surfaced in the dashboard.

**Architecture:** Extend the execution request with optional structured context. Add a dedicated risk domain/service, integrate it only when the planner selects `calculateRisk`, persist the assessment on `AgentExecution`, expose it in `ExecutionResponse`, emit audit/event evidence, and render only real persisted values in Risk Overview.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring Data JPA, Flyway, JUnit 5, Mockito, static HTML/CSS/JS dashboard.

**Spec:** `docs/superpowers/specs/2026-09-14-explainable-risk-engine-design.md`

## Global Constraints
- The LLM interprets intent; it never invents transaction facts or risk scores.
- Policy Engine remains authoritative.
- `blockCard` remains Human-in-the-Loop protected.
- Missing risk data renders `n/a`; no fabricated score.
- Existing dark/light themes and dashboard features remain intact.

---

### Task 1: Risk domain and deterministic rules
**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskLevel.java`
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskReason.java`
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskAssessmentRequest.java`
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskAssessment.java`
- Create: `src/main/java/br/com/jucelio/secureagent/risk/ExplainableRiskService.java`
- Test: `src/test/java/br/com/jucelio/secureagent/risk/ExplainableRiskServiceTest.java`

- [ ] Write failing tests for score, reasons, clamp, and level boundaries.
- [ ] Run CI and verify RED because the risk domain does not exist.
- [ ] Implement the minimal deterministic risk service.
- [ ] Run CI and verify GREEN.

### Task 2: Structured execution context and persistence
**Files:**
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/CreateExecutionRequest.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/AgentExecution.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/ExecutionResponse.java`
- Create: `src/main/resources/db/migration/V6__add_risk_assessment.sql`
- Test: `src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java`

- [ ] Write failing integration/unit test showing context reaches a real assessment.
- [ ] Verify RED.
- [ ] Add optional context and persisted risk fields.
- [ ] Verify GREEN.

### Task 3: Execution orchestration, audit, and event evidence
**Files:**
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/AgentExecutionService.java`
- Test: `src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java`

- [ ] Write/extend failing test for `calculateRisk` orchestration.
- [ ] Verify RED.
- [ ] Invoke `ExplainableRiskService`, persist assessment, emit audit/event payload.
- [ ] Verify GREEN.

### Task 4: Dashboard Risk Overview
**Files:**
- Modify: `src/main/resources/static/assets/dashboard-operations.js`
- Modify: `src/test/java/br/com/jucelio/secureagent/dashboard/DashboardResourceTest.java`

- [ ] Write failing dashboard resource assertions for persisted `riskScore`, `riskLevel`, `riskReasons`.
- [ ] Verify RED.
- [ ] Render the latest real assessment; render `n/a` when absent.
- [ ] Verify GREEN.

### Task 5: Full verification
- [ ] Run `mvn -B -ntp clean verify` in CI.
- [ ] Review PR diff for security, regressions, fake metrics, and accidental changes.
- [ ] Keep work on `feat/v1.3-spring-ai`; do not merge without explicit user choice.
