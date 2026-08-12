CREATE TABLE IF NOT EXISTS batch_log_metrics (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    from_timestamp VARCHAR(64),
    to_timestamp VARCHAR(64),
    total_logs_count BIGINT DEFAULT 0,
    avg_execution_time_ms DOUBLE PRECISION,
    minio_storage_path VARCHAR(512),
    result_data JSON,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
