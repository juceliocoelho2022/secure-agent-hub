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

## Ambiente local

As credenciais de laboratório existem apenas para desenvolvimento. Não devem ser reutilizadas em produção.

Defina um segredo JWT próprio:

```bash
export JWT_SECRET='substitua-por-um-segredo-forte'
```

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
