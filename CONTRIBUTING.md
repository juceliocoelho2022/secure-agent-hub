# Contributing

Obrigado pelo interesse no SecureAgent Hub.

## Fluxo sugerido

1. Crie uma branch a partir de `main`.
2. Use commits pequenos e objetivos.
3. Rode os testes antes de abrir PR.
4. Atualize documentação quando alterar comportamento ou arquitetura.

## Padrão de branches

```text
feat/nome-da-feature
fix/nome-do-bug
chore/nome-da-tarefa
```

## Conventional Commits

Exemplos:

```text
feat: add transactional outbox
fix: prevent duplicate tool execution
chore: improve CI pipeline
docs: document agent security model
```

## Checklist antes do PR

```bash
mvn clean verify
```

Valide também:

- nenhuma credencial foi commitada;
- endpoints protegidos têm autorização adequada;
- novas operações críticas passam pelo Policy Engine;
- eventos/consumidores são idempotentes quando aplicável;
- mudanças relevantes aparecem no README/ROADMAP.

## Filosofia

O projeto prioriza código explicável, segurança, rastreabilidade e evolução incremental. Não adicione complexidade distribuída sem um problema arquitetural claro para resolver.
