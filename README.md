# SecureAgent Hub v1.1

Projeto de portfólio para praticar **Java 21 + Spring Boot + segurança + agentes + Human-in-the-Loop + auditoria**.

<p align="center">
  <img src="docs/secure-agent-hub-dashboard.jpg" alt="SecureAgent Hub Dashboard" width="100%" />
</p>

## Evolução incluída nesta versão

A v1.1 substitui HTTP Basic/in-memory users por uma camada de segurança persistida:

- JWT Bearer Token assinado com HMAC SHA-256
- access token de curta duração
- refresh token opaco persistido e rotacionado
- refresh token armazenado somente como hash SHA-256
- usuários persistidos no PostgreSQL
- roles persistidas (`ANALYST`, `OPERATOR`, `AUDITOR`, `ADMIN`)
- RBAC aplicado aos endpoints
- endpoint `/api/v1/users/me`
- logout com revogação de refresh token
- Flyway V2 para usuários, roles e refresh tokens

## Fluxo atual

```text
Login -> JWT -> API -> Agent Planner -> Policy Engine -> Tool
                                      -> Human Approval (quando crítico)
                                      -> Audit Trail
```

Nesta etapa o agente continua sendo um `RuleBasedAgentPlanner`. Isso é intencional: primeiro fortalecemos identidade, autorização e controle. Spring AI entra na v1.3.

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Security
- OAuth2 Resource Server / JWT
- PostgreSQL 17
- Flyway
- Spring Data JPA
- Actuator
- JUnit 5 + Mockito
- JaCoCo
- Docker Compose

## Usuários de desenvolvimento

| Usuário | Senha | Papel |
|---|---|---|
| analyst | analyst123 | ANALYST |
| operator | operator123 | OPERATOR |
| auditor | auditor123 | AUDITOR |
| admin | admin123 | ADMIN |

> Criados automaticamente apenas para laboratório local. Em produção, use um IdP/OIDC (Keycloak, Cognito, Entra ID etc.) e defina `BOOTSTRAP_DEV_USERS=false`.

## Executar

```bash
docker compose up -d
mvn spring-boot:run
```

Para ambiente real, substitua o segredo default:

```bash
export JWT_SECRET='um-segredo-longo-forte-e-gerenciado-fora-do-codigo'
```

No PowerShell:

```powershell
$env:JWT_SECRET="um-segredo-longo-forte-e-gerenciado-fora-do-codigo"
mvn spring-boot:run
```

## Autenticação

### Login

`POST /api/v1/auth/login`

```json
{
  "username": "analyst",
  "password": "analyst123"
}
```

Resposta:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresIn": 900,
  "refreshToken": "..."
}
```

### Refresh com rotação

`POST /api/v1/auth/refresh`

O refresh token usado é revogado e um novo par é emitido.

### Perfil

`GET /api/v1/users/me`

Header:

```text
Authorization: Bearer <accessToken>
```

## RBAC

| Área | Acesso |
|---|---|
| Execuções de agente | usuário autenticado |
| Aprovações | OPERATOR ou ADMIN |
| Auditoria | AUDITOR ou ADMIN |
| Perfil | usuário autenticado |

## Próximas evoluções

### v1.2 — Event Driven
- Kafka
- eventos de domínio
- retry/backoff
- DLQ/DLT
- idempotência
- outbox transacional

### v1.3 — Spring AI
- ChatClient
- Tool Calling real
- structured output
- modelo configurável
- controle de tokens/custos

### v1.4 — RAG
- pgvector
- documentos/políticas
- embeddings
- retrieval

### v1.5 — Observabilidade
- OpenTelemetry
- Prometheus
- Grafana
- tracing distribuído
- métricas de agentes/tools/tokens

### v2 — Cloud
- GitHub Actions
- ECR
- Terraform
- AWS
- Kubernetes/EKS

## Princípio arquitetural

> O LLM pode interpretar a intenção. O backend controla a execução.
