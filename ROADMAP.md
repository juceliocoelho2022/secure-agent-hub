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

## ✅ v1.2 — Event Driven
- Apache Kafka
- eventos de domínio com `DomainEventEnvelope`
- transactional outbox
- identidade única `eventId` do outbox ao consumer
- producer idempotente
- consumer idempotente
- `processed_events` para deduplicação
- retry com exponential backoff
- Dead Letter Topic (`secure-agent.events.DLT`)
- correlação de eventos
- testes automatizados do envelope, consumer e outbox publisher
- documentação de contratos e delivery semantics em `docs/EVENTS.md`

### Hardening futuro da camada de eventos
- chave composta `(consumer_name, event_id)` quando houver múltiplos consumers independentes
- row claiming / locking para múltiplas réplicas do outbox publisher
- publicação não bloqueante no outbox publisher
- política limitada de falha/retry do publisher
- provisionamento explícito de tópicos Kafka
- propagação de request correlation ID e causation ID
- testes de compatibilidade/versionamento de contratos

## ▶ v1.3 — Spring AI
- ChatClient
- Tool Calling
- structured output
- AI gateway / provider abstraction
- integração do planejamento probabilístico com Policy Engine determinístico
- token/cost metrics
- proteção contra execução direta de tools pelo modelo

## v1.4 — RAG
- pgvector
- ingestão de documentos
- embeddings
- retrieval
- policies knowledge base

## v1.5 — Observabilidade / AgentOps
- OpenTelemetry
- Prometheus
- Grafana
- tracing distribuído
- métricas de agentes, tools, tokens e aprovações
- SLOs e alertas

## v2.0 — Cloud
- GitHub Actions
- ECR
- Terraform
- AWS
- EKS
- secrets manager
