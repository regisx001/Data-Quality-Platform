CREATE TABLE IF NOT EXISTS batch_log_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    from_timestamp VARCHAR(64),
    to_timestamp VARCHAR(64),
    total_logs_count BIGINT DEFAULT 0,
    avg_execution_time_ms DOUBLE PRECISION,
    minio_storage_path VARCHAR(512),
    result_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_batch_log_metrics_created_at ON batch_log_metrics(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_batch_log_metrics_job_id ON batch_log_metrics(job_id);
