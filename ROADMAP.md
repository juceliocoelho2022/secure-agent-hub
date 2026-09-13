# SecureAgent Hub — Roadmap incremental

## ✅ v1.0 — Base segura
- Spring Boot + PostgreSQL
- RuleBased Agent Planner
- Policy Engine
- Human-in-the-Loop
- Audit Trail

## ✅ v1.1 — JWT + RBAC persistido
- usuários/roles no PostgreSQL
- JWT Bearer
- refresh token persistido, hashed e rotacionado
- RBAC
- `/users/me`
- revogação de refresh token

## ▶ v1.2 — Event Driven
- Kafka
- eventos de domínio
- transactional outbox
- idempotência
- retry/backoff
- DLQ/DLT
- consumer deduplication

## v1.3 — Spring AI
- ChatClient
- Tool Calling
- structured output
- AI gateway abstraction
- token/cost metrics

## v1.4 — RAG
- pgvector
- ingestão de documentos
- embeddings
- retrieval
- policies knowledge base

## v1.5 — Observabilidade
- OpenTelemetry
- Prometheus
- Grafana
- tracing
- SLOs e alertas

## v2.0 — Cloud
- GitHub Actions
- ECR
- Terraform
- AWS
- EKS
- secrets manager
