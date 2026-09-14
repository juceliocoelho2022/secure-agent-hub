# Governed Risk Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn CRITICAL explainable risk assessments into a persisted `BLOCK_CARD` recommendation that is routed through Policy Engine and mandatory human approval before controlled execution.

**Architecture:** Extend the existing risk execution path rather than creating an autonomous remediation subsystem. A small deterministic `RiskRecommendationService` maps `RiskAssessment` to an optional recommendation; `AgentExecutionService` persists it, evaluates `blockCard` through `PolicyService`, and reuses the existing approval workflow. Dashboard rendering consumes persisted execution metadata only.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring Data JPA, PostgreSQL/Flyway, JUnit 5, Mockito, vanilla JS/CSS.

**Spec:** `docs/superpowers/specs/2026-09-14-risk-recommendation-governance-design.md`

## Global Constraints
- CRITICAL risk may recommend `blockCard` but must never execute it automatically.
- `PolicyService` remains authoritative.
- `blockCard` must remain Human-in-the-Loop protected.
- Recommendation data must be persisted and exposed through the existing execution API.
- Dashboard must render `n/a` if no real persisted recommendation exists.

---

### Task 1: Recommendation domain

**Files:**
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskRecommendation.java`
- Create: `src/main/java/br/com/jucelio/secureagent/risk/RiskRecommendationService.java`
- Test: `src/test/java/br/com/jucelio/secureagent/risk/RiskRecommendationServiceTest.java`

**Interfaces:**
- Consumes: `RiskAssessment`
- Produces: `Optional<RiskRecommendation> recommend(RiskAssessment assessment)`

- [ ] Write failing tests for CRITICAL -> `blockCard/CRITICAL_RISK` and HIGH -> empty.
- [ ] Verify RED in CI.
- [ ] Implement minimal deterministic recommendation service.
- [ ] Verify GREEN.

### Task 2: Governed execution integration

**Files:**
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/AgentExecution.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/AgentExecutionService.java`
- Modify: `src/main/java/br/com/jucelio/secureagent/execution/ExecutionResponse.java`
- Modify: `src/test/java/br/com/jucelio/secureagent/execution/AgentExecutionServiceTest.java`
- Create: `src/main/resources/db/migration/V7__add_risk_recommendation.sql`

**Interfaces:**
- Persists: `recommendedAction`, `recommendationReason`
- Reuses: `PolicyService.evaluate("blockCard")`, `ApprovalRequest`, existing approval execution path.

- [ ] Write failing service test proving CRITICAL risk becomes WAITING_APPROVAL and tool executor is not called.
- [ ] Verify RED.
- [ ] Persist recommendation metadata and create V7 migration.
- [ ] Integrate recommendation after risk assessment and route through policy/HITL.
- [ ] Emit `RISK_ACTION_RECOMMENDED` audit/event.
- [ ] Verify GREEN.

### Task 3: Dashboard Recommended Action

**Files:**
- Modify: `src/main/resources/static/dashboard.html`
- Modify: `src/main/resources/static/assets/dashboard-operations.js`
- Modify: `src/main/resources/static/assets/dashboard-visual.css`
- Modify: `src/test/java/br/com/jucelio/secureagent/dashboard/DashboardResourceTest.java`

**Interfaces:**
- Consumes execution fields: `recommendedAction`, `recommendationReason`, `riskScore`, `riskLevel`, `status`.

- [ ] Write failing dashboard resource test for `recommendedAction` rendering.
- [ ] Verify RED.
- [ ] Add Recommended Action card and real-data renderer.
- [ ] Keep neutral `n/a` state when unavailable.
- [ ] Verify full `mvn clean verify` in CI.
