# Governed Risk Recommendation Design

## Goal
Connect explainable risk assessment to a governed remediation recommendation without allowing the risk engine or LLM to execute critical actions automatically.

## Core invariant
A CRITICAL risk assessment may recommend `blockCard`, but it never authorizes or executes it. The Policy Engine remains authoritative and `blockCard` still requires Human-in-the-Loop approval.

## Flow
`RiskAssessment -> RiskRecommendationService -> BLOCK_CARD/CRITICAL_RISK -> PolicyService(blockCard) -> REQUIRE_APPROVAL -> ApprovalRequest -> human approve/reject -> ToolExecutor`.

## Threshold
Only `RiskLevel.CRITICAL` (score 80..100) creates the v1 recommendation. LOW, MEDIUM and HIGH assessments complete normally without a critical-action recommendation.

## Execution state
The same `AgentExecution` persists both risk evidence and recommendation provenance. For CRITICAL risk, after assessment the execution transitions to `WAITING_APPROVAL` with governed tool `blockCard`; it is not marked complete until the human decision resolves the protected action. Approval executes `blockCard` through the existing backend ToolExecutor. Rejection terminates the execution as REJECTED.

## Persistence/API
Persist nullable `recommended_action` and `recommendation_reason` on `agent_executions` and expose them through `ExecutionResponse`. Existing `risk_score`, `risk_level` and `risk_reasons` remain the source of truth for risk evidence.

## Audit/events
Emit an audit action `RISK_ACTION_RECOMMENDED` and domain event `agent.risk.action-recommended` carrying execution id, risk score, risk level, recommended action and reason. Existing approval/audit/event machinery remains responsible for the later human decision and controlled execution.

## Dashboard
Add a `Recommended Action` card showing the latest execution with recommendation metadata. It must show action, reason, risk, policy state and human/execution state from persisted execution data. When no recommendation exists, show `n/a` rather than fabricated state.

## Security
- No direct LLM tool execution.
- No automatic block on score threshold.
- `PolicyService` must evaluate `blockCard` before any approval request is created.
- Human approval remains mandatory for `blockCard`.
- Existing RBAC restrictions on approval endpoints are unchanged.
