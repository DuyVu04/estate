CREATE TABLE workflow_instances (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,

    workflow_name VARCHAR(100) NOT NULL,

    target_id VARCHAR(64) NOT NULL,

    status VARCHAR(30) NOT NULL,

    current_step VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_workflow_name_target UNIQUE (workflow_name, target_id)
);

CREATE TABLE workflow_histories (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,

    workflow_instance_id VARCHAR(36) NOT NULL,

    action VARCHAR(50) NOT NULL,

    step VARCHAR(100) NOT NULL,

    previous_status VARCHAR(50),

    new_status VARCHAR(50),

    performed_by VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_workflow_history_instance
        FOREIGN KEY (workflow_instance_id)
        REFERENCES workflow_instances(id)
        ON DELETE CASCADE
);
