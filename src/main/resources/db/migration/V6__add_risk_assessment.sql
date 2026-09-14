ALTER TABLE agent_executions
    ADD COLUMN risk_score INTEGER,
    ADD COLUMN risk_level VARCHAR(16),
    ADD COLUMN risk_reasons VARCHAR(1000);
