ALTER TABLE agent_executions
    ADD COLUMN recommended_action VARCHAR(120),
    ADD COLUMN recommendation_reason VARCHAR(120);
