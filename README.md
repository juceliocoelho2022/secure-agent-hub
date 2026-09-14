# 🛡️ SecureAgent Hub

> **Secure AI Agent Execution Platform** — Java 21, Spring Boot, Spring AI, security, policy enforcement, Human-in-the-Loop and event-driven architecture.

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.8-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9.1-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![CI](https://github.com/juceliocoelho2022/secure-agent-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/juceliocoelho2022/secure-agent-hub/actions/workflows/ci.yml)

O **SecureAgent Hub** é um projeto de engenharia para estudar como colocar agentes de IA em produção sem entregar controle irrestrito ao modelo. A plataforma separa interpretação de intenção, autorização e execução real, aplicando **JWT/RBAC, Policy Engine, Human-in-the-Loop, auditoria, Kafka e controles explícitos para uso de LLMs**.

> **Princípio arquitetural:** o LLM interpreta. O Risk Engine calcula. O Policy Engine autoriza. O humano aprova ações críticas. O backend executa.

---

## 🎯 Visão do produto

<p align="center">
  <img src="https://raw.githubusercontent.com/juceliocoelho2022/secure-agent-hub/main/docs/secure-agent-hub-dashboard.jpg" alt="SecureAgent Hub Dashboard Concept" width="100%" />
</p>

A aplicação inclui um dashboard operacional para acompanhar execuções, aprovações Human-in-the-Loop e timeline governada.

---

## Estado atual — v1.3 Governed Spring AI + Explainable Risk
| Capacidade | Status |
|---|---|
| Java 21 + Spring Boot 3.5.5 | ✅ |
| PostgreSQL 17 + Flyway | ✅ |
| JWT + refresh token rotation | ✅ |
| RBAC (`ANALYST`, `OPERATOR`, `AUDITOR`, `ADMIN`) | ✅ |
| Policy Engine | ✅ |
| Human-in-the-Loop | ✅ |
| Audit Trail | ✅ |
| Apache Kafka 3.9.1 | ✅ |
| Transactional Outbox | ✅ |
| Consumer idempotente + retry/DLT | ✅ |
| Dashboard operacional + Execution Inspector | ✅ |
| Spring AI 1.1.8 foundation | ✅ |
| Structured AI planning | ✅ |
| Governed tool allowlist | ✅ |
| Deterministic fallback | ✅ |
| Explainable Risk Engine | ✅ |
| Score, nível e reasons persistidos | ✅ |
| Recomendação governada para risco crítico | ✅ |
| `blockCard` exige Policy Engine + HITL | ✅ |
| Recommended Action no dashboard | ✅ |
| Tema Dark / Light persistente | ✅ |
| Automatic LLM tool execution | 🚫 desabilitado |
| RAG + pgvector | v1.4 NEXT |
| OpenTelemetry + Prometheus + Grafana | 🗺️ v1.5 |
| AWS + Terraform + Kubernetes | 🗺️ v2.0 |

---

## 🏗️ Arquitetura

```text
User Prompt
    ↓
REST API / Spring Security
    ↓
AgentPlanner
   ↙                         ↘
RuleBasedAgentPlanner     SpringAiAgentPlanner
(default / fallback)      (structured proposal)
   \                         /
            AgentPlan
               ↓
          Policy Engine
          ↙    ↓     ↘
       ALLOW  HITL   DENY
         ↓      ↓
         ↓   Approve/Reject
         \      /
          ToolExecutor
               ↓
         Audit + Domain Events
               ↓
    Transactional Outbox → Kafka
```

A integração Spring AI foi deliberadamente construída **antes** da execução de tools. O `ChatClient` produz apenas uma proposta estruturada. A saída é validada contra um catálogo de ferramentas do backend e, somente depois, passa pelo mesmo `PolicyService` já utilizado pelo fluxo determinístico.

Para operações protegidas como `blockCard`, a origem `SPRING_AI` não altera a regra: a execução permanece em `WAITING_APPROVAL` até uma decisão humana autorizada.

---


## Explainable Risk + Governed Recommendation

A v1.3 adiciona um **Risk Engine determinístico e explicável**. O LLM não calcula score e não inventa fatos da transação. O backend recebe contexto estruturado, aplica regras testáveis e persiste `riskScore`, `riskLevel` e `riskReasons`.

```text
Structured Transaction Context
            ↓
      calculateRisk
            ↓
  ExplainableRiskService
            ↓
 score + level + reasons
            ↓
 RiskRecommendationService
            ↓
 CRITICAL → blockCard / CRITICAL_RISK
            ↓
       Policy Engine
            ↓
     REQUIRE_APPROVAL
            ↓
    Human-in-the-Loop
      ↙             ↘
   REJECT          APPROVE
                     ↓
                ToolExecutor
                     ↓
                  COMPLETED
```

**Invariante de segurança:** recomendação não é autorização. Mesmo em risco `CRITICAL`, `blockCard` não é executado automaticamente.

### Cenário de demonstração — TX-9001

```json
{
  "agent": "fraud-agent",
  "prompt": "Calcule o risco da operação TX-9001",
  "context": {
    "transactionId": "TX-9001",
    "amount": 9800.00,
    "country": "US",
    "usualCountry": "BR",
    "hour": 2,
    "rapidRetry": true,
    "knownDevice": false
  }
}
```

Resultado esperado:

```text
95 / CRITICAL
HIGH_AMOUNT
FOREIGN_COUNTRY
UNUSUAL_HOUR
RAPID_RETRY

Recommended Action: blockCard
Reason: CRITICAL_RISK
Policy: REQUIRE_APPROVAL
Human: PENDING
Execution: NOT_EXECUTED
```

Após aprovação humana autorizada, o backend executa a tool protegida e mantém a trilha em auditoria e eventos.

---
## Spring AI Controlled Planner
Existem dois modos:

```text
AGENT_PLANNER=rules      → RuleBasedAgentPlanner
AGENT_PLANNER=spring-ai  → SpringAiAgentPlanner
```

O modo padrão é `rules`, permitindo desenvolvimento e CI sem API key.

No modo AI:

```text
ChatClient
   ↓
AiToolProposal
   ↓
ToolCatalog allowlist
   ↓
AgentPlan(source=SPRING_AI)
   ↓
Policy Engine
```

Falhas do provedor, resposta nula ou tool não permitida acionam fallback para o `RuleBasedAgentPlanner`. O modelo não recebe `ToolExecutor`, callbacks de execução ou autoridade de aprovação.

📚 Veja [`docs/SPRING_AI.md`](docs/SPRING_AI.md).

---

## ⚡ Event-Driven Architecture

```text
Business transaction
       ↓
DomainEventService
       ↓
Transactional Outbox (PostgreSQL)
       ↓
OutboxPublisher
       ↓
secure-agent.events (Kafka)
       ↓
DomainEventConsumer
   ↙                 ↘
sucesso             falha
  ↓                   ↓
processed_events    retry / backoff
                      ↓
              secure-agent.events.DLT
```

A arquitetura assume **at-least-once delivery** e trata duplicação explicitamente no consumer. Não há alegação de exactly-once end-to-end.

---

## 🔐 Segurança por design

- JWT Bearer + refresh token rotacionado;
- RBAC e princípio de least privilege;
- Policy Engine antes da execução;
- Human-in-the-Loop para operações críticas;
- auditoria e provenance do planner (`RULE_BASED` / `SPRING_AI`);
- allowlist de tools conhecida pelo backend;
- automatic Spring AI tool calling desabilitado;
- fallback determinístico para falha/malformed AI output;
- segredos configurados por ambiente;
- eventos assíncronos não contornam políticas;
- retry/backoff e Dead Letter Topic no pipeline Kafka.

Consulte [`SECURITY.md`](SECURITY.md).

---

## 🧰 Stack

**Backend:** `Java 21` · `Spring Boot 3.5.5` · `Spring AI 1.1.8` · `Spring Security` · `Spring Data JPA`

**Dados & Eventos:** `PostgreSQL 17` · `Flyway` · `Apache Kafka 3.9.1` · `Transactional Outbox`

**Qualidade & Operação:** `JUnit 5` · `Mockito` · `JaCoCo` · `Docker Compose` · `GitHub Actions`

---

## 🚀 Executando localmente

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker / Docker Compose

### Infraestrutura

```bash
docker compose up -d
```

Portas locais: aplicação `8080`, PostgreSQL `5433`, Kafka `9092`.

### Modo seguro padrão — sem OpenAI API key

PowerShell:

```powershell
Remove-Item Env:OPENAI_API_KEY -ErrorAction SilentlyContinue
$env:AGENT_PLANNER="rules"
$env:SPRING_AI_CHAT_MODEL="none"
mvn spring-boot:run
```

Validação:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

### Modo Spring AI

Nunca versione uma API key. Configure-a somente no ambiente local/secret manager:

```powershell
$env:AGENT_PLANNER="spring-ai"
$env:SPRING_AI_CHAT_MODEL="openai"
$env:OPENAI_API_KEY="<set-locally-not-in-git>"
$env:OPENAI_CHAT_MODEL="gpt-5-mini"
mvn spring-boot:run
```

Neste modo o modelo **propõe** uma ferramenta; ele não a executa diretamente.

### Build completo

```bash
mvn -B -ntp clean verify
```

O CI executa o mesmo `clean verify` em Java 21 e publica JaCoCo como artifact.

---

## 👥 Usuários de laboratório

| Usuário | Senha | Papel |
|---|---|---|
| `analyst` | `analyst123` | ANALYST |
| `operator` | `operator123` | OPERATOR |
| `auditor` | `auditor123` | AUDITOR |
| `admin` | `admin123` | ADMIN |

⚠️ Credenciais exclusivamente para laboratório local. Em produção, use IdP/OIDC e `BOOTSTRAP_DEV_USERS=false`.

---

## 🧑‍⚖️ RBAC

| Área | Acesso |
|---|---|
| Execuções de agente | usuário autenticado |
| Timeline operacional | `OPERATOR`, `AUDITOR`, `ADMIN` |
| Aprovações | `OPERATOR` ou `ADMIN` |
| Auditoria completa | `AUDITOR` ou `ADMIN` |
| Perfil | usuário autenticado |

---

## 🛣️ Roadmap

```text
v1.0  Secure Foundation                 ✅
  ↓
v1.1  JWT / Persisted RBAC              ✅
  ↓
v1.2  Event-Driven / Kafka              ✅
  ↓
v1.3  Governed Spring AI + Risk/HITL    DONE
  ↓
v1.4  RAG / pgvector                    NEXT
  ↓
v1.5  Observability / AgentOps          🗺️
  ↓
v2.0  AWS / Terraform / EKS             🗺️
```

---

## 📖 Documentação

- [Arquitetura](docs/ARCHITECTURE.md)
- [Spring AI Controlled Planner](docs/SPRING_AI.md)
- [Eventos e delivery semantics](docs/EVENTS.md)
- [Roadmap](ROADMAP.md)
- [Política de Segurança](SECURITY.md)
- [Como contribuir](CONTRIBUTING.md)
- [Exemplos HTTP](http-requests.http)

---

## 👨‍💻 Autor

**Jucelio Farias Coelho**  
Java Backend · Spring Boot · Sistemas Distribuídos · Cloud · IA aplicada

Projeto de portfólio focado na interseção de **engenharia backend, sistemas distribuídos, segurança e agentes de IA governados**.
