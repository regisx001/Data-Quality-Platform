CREATE TABLE IF NOT EXISTS realtime_log_metrics (
    id UUID PRIMARY KEY,
    window_start VARCHAR(64) NOT NULL,
    window_end VARCHAR(64) NOT NULL,
    throughput_logs_per_sec DOUBLE PRECISION NOT NULL,
    total_logs_count BIGINT NOT NULL,
    info_count BIGINT DEFAULT 0,
    warn_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    debug_count BIGINT DEFAULT 0,
    avg_execution_time_ms DOUBLE PRECISION,
    service_breakdown TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_realtime_log_metrics_window ON realtime_log_metrics(window_start, window_end);
