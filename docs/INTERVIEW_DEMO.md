# SecureAgent Hub — Roteiro de demonstração para entrevista

## Pitch de 60 segundos

O SecureAgent Hub é uma plataforma Java 21 e Spring Boot para execução governada de agentes de IA. Eu separei interpretação probabilística de autorização e execução determinísticas. O planner pode usar Spring AI para interpretar intenção, mas qualquer tool proposta é validada pelo backend e passa pelo Policy Engine. Para operações críticas existe Human-in-the-Loop. Também implementei um Risk Engine explicável: fatos estruturados da transação geram score e reasons determinísticos. Um risco CRITICAL pode recomendar `blockCard`, porém nunca executa automaticamente; a Policy exige aprovação humana. As decisões ficam auditáveis e eventos são publicados por Transactional Outbox para Kafka.

> **O LLM interpreta. O Risk Engine calcula. O Policy Engine autoriza. O humano aprova ações críticas. O backend executa.**

## Demonstração principal — TX-9001

### 1. Login
`operator / operator123`

### 2. Criar avaliação
`Calcule o risco da operação TX-9001`

```text
Transaction ID: TX-9001
Amount: 9800
Country: US
Usual country: BR
Hour: 2
Rapid retry: true
Known device: false
```

### 3. Explicar o score

```text
HIGH_AMOUNT       +35
FOREIGN_COUNTRY   +25
UNUSUAL_HOUR      +20
RAPID_RETRY       +15
---------------------
TOTAL              95
```

Resultado:

```text
Risk Score: 95 / 100
Risk Level: CRITICAL
Reasons: HIGH_AMOUNT, FOREIGN_COUNTRY, UNUSUAL_HOUR, RAPID_RETRY
```

### 4. Recomendação governada

```text
Recommended Action: blockCard
Recommendation Reason: CRITICAL_RISK
Policy: REQUIRE_APPROVAL
```

### 5. Human-in-the-Loop

Antes da aprovação:

```text
Status: WAITING_APPROVAL
Human: PENDING
Execution: NOT_EXECUTED
```

Mesmo com score 95, `blockCard` não é executado sem decisão explícita de um operador autorizado.

### 6. Aprovar

```text
EXECUTION_CREATED
      ↓
TOOL_PLANNED
      ↓
RISK_ASSESSED
      ↓
RISK_ACTION_RECOMMENDED
      ↓
HUMAN_APPROVAL_REQUIRED
      ↓
HUMAN_APPROVED
      ↓
TOOL_EXECUTED
      ↓
COMPLETED
```

### 7. Trilha assíncrona

```text
Domain Event → Transactional Outbox → Kafka → Idempotent Consumer → processed_events
                                                ↓ failure
                                           retry/backoff → DLT
```

## Perguntas de entrevista

### Por que o LLM não executa tools diretamente?
Porque interpretação probabilística não deve equivaler a autorização. O backend valida a proposta e aplica política determinística.

### Por que Human-in-the-Loop?
Porque ações críticas possuem impacto operacional e exigem uma fronteira explícita de autoridade, RBAC e auditoria.

### Por que Risk Engine determinístico?
Por explicabilidade, reprodutibilidade e testabilidade. Um modelo ML futuro pode entrar atrás do mesmo contrato sem remover Policy/HITL.

### E se o provider de IA falhar?
O planner Spring AI falha de forma segura e usa fallback determinístico. A falha não contorna controles de segurança.

### Por que Transactional Outbox?
Para reduzir dual-write entre PostgreSQL e Kafka sem depender de transação distribuída.

### É exactly-once?
Não. A arquitetura assume at-least-once e trata duplicação com consumer idempotente.

### Próximo passo?
v1.4 adiciona RAG/pgvector preservando a mesma fronteira Policy/HITL/controlled execution.