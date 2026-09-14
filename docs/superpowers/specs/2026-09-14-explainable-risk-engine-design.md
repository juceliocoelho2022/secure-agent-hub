# Explainable Risk Engine Design

## Goal
Replace the mock `calculateRisk` behavior with a deterministic, explainable backend risk engine driven by structured execution context, while preserving the architecture principle: the LLM interprets intent, the backend calculates risk, the Policy Engine authorizes, and the backend executes.

## Request contract
`POST /api/v1/agents/executions` keeps `agent` and `prompt` and adds an optional `context` object carrying structured facts such as transaction id, amount, country, usual country, hour, rapid retry and known device. In the local dashboard these values are demo inputs. In production they must be sourced from, or verified against, trusted server-side systems before being used for risk decisions.

## Risk model
The first version is deterministic and auditable. Signals:
- HIGH_AMOUNT +35
- FOREIGN_COUNTRY +25
- UNUSUAL_HOUR +20
- RAPID_RETRY +15
- KNOWN_DEVICE -10

Score is clamped to 0..100. Levels:
- 0..29 LOW
- 30..59 MEDIUM
- 60..79 HIGH
- 80..100 CRITICAL

## Domain boundary
Create `RiskAssessmentRequest`, `RiskAssessment`, `RiskLevel`, `RiskReason` and `ExplainableRiskService` under a dedicated risk package. The risk engine never parses free-form prompt text to invent transaction facts.

## Execution integration
When the selected tool is `calculateRisk`, `AgentExecutionService` maps structured context into `RiskAssessmentRequest`, invokes the risk service, persists score/level/reasons on `AgentExecution`, emits an audit entry and domain event, and completes the execution with a structured human-readable result.

## Persistence and API
Add nullable `risk_score`, `risk_level`, and `risk_reasons` columns to `agent_executions`. Expose those fields through `ExecutionResponse` so dashboard and inspector consume the same persisted result.

## Dashboard
`Risk Overview` reads real persisted execution risk metadata. It must show `n/a` when no real assessment exists. It must never display the legacy mock score.

## Security and governance
No change to RBAC, HITL, Policy Engine authority, planner selection, or controlled tool execution. `blockCard` remains human-approval protected. Browser-supplied demo context is not a production trust boundary; production risk facts must be obtained or verified server-side.
