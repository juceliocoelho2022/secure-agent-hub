CREATE TABLE agent_executions (
    entity_id UUID PRIMARY KEY,
    id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    agent_name VARCHAR(80) NOT NULL,
    prompt VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_tool VARCHAR(120),
    result VARCHAR(4000)
);

CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    tool_name VARCHAR(120) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    decided_by VARCHAR(120)
);

CREATE INDEX idx_approval_status ON approval_requests(status);
CREATE INDEX idx_approval_execution ON approval_requests(execution_id);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    actor VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    details VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_execution ON audit_events(execution_id, created_at);
