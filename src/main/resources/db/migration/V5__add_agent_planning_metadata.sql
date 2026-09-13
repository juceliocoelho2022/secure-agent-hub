ALTER TABLE agent_executions
    ADD COLUMN planner_source VARCHAR(32),
    ADD COLUMN prompt_tokens INTEGER,
    ADD COLUMN completion_tokens INTEGER,
    ADD COLUMN total_tokens INTEGER;
