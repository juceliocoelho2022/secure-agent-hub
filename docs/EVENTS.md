# SecureAgent Hub — Event-Driven Architecture

## Purpose

SecureAgent Hub uses domain events to decouple agent execution from asynchronous processing while preserving security, auditability and failure recovery.

The v1.2 pipeline is:

```text
HTTP request
    |
    v
AgentExecutionService
    |
    +-- business transaction
    |
    +-- DomainEventService
            |
            v
      outbox_events (PostgreSQL)
            |
            v
      OutboxPublisher
            |
            v
      secure-agent.events (Kafka)
            |
            v
      DomainEventConsumer
        |           |
        | success   | failure
        v           v
 processed_events  retry + exponential backoff
                    |
                    v
             secure-agent.events.DLT
```

## Domain event envelope

Every new domain event is serialized using a standard envelope:

```json
{
  "eventId": "UUID",
  "eventType": "agent.execution.created",
  "aggregateType": "AgentExecution",
  "aggregateId": "UUID",
  "occurredAt": "2026-09-13T19:16:14Z",
  "correlationId": "UUID",
  "causationId": null,
  "schemaVersion": 1,
  "payload": {}
}
```

### Fields

| Field | Purpose |
|---|---|
| `eventId` | Globally unique event identity and idempotency key |
| `eventType` | Semantic name of the event |
| `aggregateType` | Domain aggregate that produced the event |
| `aggregateId` | Aggregate instance identifier |
| `occurredAt` | UTC event occurrence timestamp |
| `correlationId` | Correlates events from the same execution flow |
| `causationId` | Optional identifier of the event/command that caused this event |
| `schemaVersion` | Contract version for event evolution |
| `payload` | Event-specific business data |

In v1.2, `correlationId` is the `AgentExecution` identifier and `causationId` is currently nullable.

## Event catalog

Current agent execution events include:

| Event | Meaning |
|---|---|
| `agent.execution.created` | An agent execution was created |
| `agent.tool.planned` | The planner selected a tool operation |
| `agent.tool.denied` | Policy Engine denied a proposed tool operation |
| `agent.approval.requested` | A critical operation requires Human-in-the-Loop approval |
| `agent.execution.completed` | The execution completed without pending human approval |
| `agent.tool.approved-and-executed` | A human-approved tool operation was executed |
| `agent.approval.rejected` | A human reviewer rejected the proposed operation |

`agent.test.failure` is reserved for controlled local validation of retry/DLT behavior and is not a business-domain event.

## Transactional outbox

Business events are first persisted to PostgreSQL in `outbox_events`. The outbox record and the business operation participate in the application transaction, preventing the classic dual-write problem where database state is committed but publication to Kafka is lost.

`OutboxPublisher` polls unpublished records and publishes them to `secure-agent.events`. After successful publication it sets `published_at`. Failed publication attempts remain available for another polling cycle and store diagnostic information in the outbox row.

The outbox `id` and envelope `eventId` are the same UUID, preserving event identity from persistence through Kafka.

## Delivery semantics

The architecture is intentionally designed for **at-least-once delivery** rather than assuming exactly-once business processing.

Kafka producer idempotence is enabled (`acks=all`, `enable.idempotence=true`). This reduces duplicate records caused by producer retries, but it does not remove the need for consumer-side idempotency.

Duplicate delivery can still occur, for example when a message is published successfully but the publisher fails before marking the outbox row as published.

## Consumer idempotency

`DomainEventConsumer` uses `eventId` as the deduplication key.

Before applying processing logic it checks `processed_events`. If the event was already processed, the duplicate is ignored. A successfully handled event is recorded in `processed_events` inside the consumer transaction.

This implements an idempotent-consumer pattern for the current `secure-agent-domain-events` consumer.

Current v1.2 scope uses `event_id` as the table primary key. If multiple independent consumer projections are introduced, evolve this to a composite identity such as `(consumer_name, event_id)`.

## Retry and exponential backoff

Consumer failures are handled by Spring Kafka `DefaultErrorHandler`.

The current retry policy uses exponential backoff:

- initial interval: 1 second
- multiplier: 2.0
- maximum interval: 8 seconds
- maximum elapsed time: 15 seconds

Transient failures therefore receive controlled retry opportunities without creating a tight retry loop.

## Dead Letter Topic

When consumer processing continues to fail after the configured retry/backoff policy, `DeadLetterPublishingRecoverer` publishes the original Kafka record to:

```text
secure-agent.events.DLT
```

The failed event is not inserted into `processed_events`, because it did not complete successfully.

This allows failed records to be inspected, diagnosed and later replayed through a controlled operational procedure.

## Failure injection

For local resilience testing, the consumer supports a controlled failure event type configured by:

```text
AGENT_EVENTS_FAILURE_INJECTION_TYPE
```

Default:

```text
agent.test.failure
```

When this event is received, the consumer deliberately throws an exception so the retry/backoff/DLT pipeline can be verified end-to-end. Do not use this event type as a production business event.

## Observed v1.2 validation

The local v1.2 validation demonstrated:

- PostgreSQL/Flyway schema version 4
- Kafka producer idempotence
- standardized `DomainEventEnvelope`
- transactional outbox publication
- consumption from `secure-agent.events`
- persisted consumer deduplication in `processed_events`
- failed events excluded from `processed_events`
- retry/backoff recovery path
- publication to `secure-agent.events.DLT`

## Known limitations / next hardening steps

The current implementation is deliberately portfolio-sized. Enterprise hardening can add:

1. `(consumer_name, event_id)` composite deduplication identity for multiple consumers.
2. Outbox row claiming/locking (`FOR UPDATE SKIP LOCKED` or equivalent) for multiple application replicas.
3. Non-blocking publication strategy instead of waiting on `KafkaTemplate.send(...).get()` inside the polling transaction.
4. A bounded publisher retry/dead-letter policy for failures between the outbox and Kafka. The current Kafka DLT protects the consumer side.
5. Explicit Kafka topic provisioning rather than relying on broker auto-creation in local development.
6. Request-level correlation ID propagation across HTTP, domain events and traces.
7. `causationId` propagation.
8. Event-contract compatibility/versioning tests.
9. Splitting compound events such as `agent.tool.approved-and-executed` into more precise domain events where required.

## Architectural principle

The event layer does not bypass SecureAgent Hub governance. The core principle remains:

> The LLM may interpret intent. The backend controls execution.

Policy Engine decisions, Human-in-the-Loop approval, auditability and idempotent asynchronous processing remain backend responsibilities.
