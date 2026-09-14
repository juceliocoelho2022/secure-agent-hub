# Security Policy

## Escopo

O SecureAgent Hub é um projeto de estudo e portfólio com foco explícito em execução segura de agentes de IA.

## Princípios

- privilégios mínimos;
- autenticação e autorização antes de qualquer operação protegida;
- Human-in-the-Loop para operações críticas;
- segredos fora do código-fonte;
- rastreabilidade de decisões e execuções;
- validação de entrada e políticas antes de Tool Calling;
- nenhuma confiança implícita em saída de LLM.

## Fronteira de confiança do Spring AI

A integração de Spring AI segue esta fronteira:

```text
ChatClient -> structured proposal only
Policy Engine -> authorization
Human Approval -> critical-action gate
ToolExecutor -> backend-controlled execution
```

O `ChatClient` não recebe callbacks de execução nem acesso ao `ToolExecutor`. O automatic tool calling permanece desabilitado por configuração.

A saída do modelo é considerada **não confiável**. Antes de virar uma ação, a proposta passa por:

1. conversão para `AiToolProposal` estruturado;
2. validação contra `ToolCatalog` allowlist;
3. `PolicyService`;
4. Human-in-the-Loop quando a política exigir;
5. execução controlada pelo backend.

Uma proposta de `blockCard` originada por `SPRING_AI` continua em `WAITING_APPROVAL` até aprovação explícita de um perfil autorizado.

## Prompt injection e tool injection

Prompts podem tentar instruir o modelo a ignorar regras, inventar ferramentas, revelar segredos ou afirmar que uma ação já foi executada. As principais contenções atuais são:

- system prompt declara o modelo como planner, não executor;
- lista de tools é definida pelo backend;
- nomes não permitidos causam fallback determinístico;
- falhas do provedor ou respostas nulas causam fallback;
- Policy Engine é executado depois do planner;
- ações críticas exigem aprovação humana;
- provenance `RULE_BASED` ou `SPRING_AI` é registrado na etapa `TOOL_PLANNED`.

Esses controles reduzem risco, mas não tornam prompt injection impossível. RAG, novos tools e futuras integrações MCP devem preservar a mesma fronteira de autorização.

## Ambiente local

As credenciais de laboratório existem apenas para desenvolvimento. Não devem ser reutilizadas em produção.

Defina um segredo JWT próprio:

```bash
export JWT_SECRET='substitua-por-um-segredo-forte'
```

API keys de provedores de IA devem ser fornecidas por variável de ambiente ou secret manager. Nunca versione `OPENAI_API_KEY`.

Em produção, a evolução prevista é utilizar um IdP/OIDC como Keycloak, Amazon Cognito ou Microsoft Entra ID.

## Reporte de vulnerabilidades

Não publique credenciais, tokens ou dados sensíveis em issues. Ao encontrar uma vulnerabilidade, descreva o cenário de forma responsável e sem expor segredos reais.

## Ameaças que o projeto pretende tratar

- execução não autorizada de tools;
- privilege escalation;
- replay de operações;
- duplicidade de mensagens/eventos;
- prompt/tool injection;
- vazamento de segredos;
- ações críticas sem aprovação;
- ausência de auditoria;
- abuso de tokens/custos de IA.
