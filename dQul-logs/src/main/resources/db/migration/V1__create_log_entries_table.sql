CREATE TABLE IF NOT EXISTS log_entries (
    id UUID PRIMARY KEY,
    trace_id VARCHAR(64),
    service_name VARCHAR(64) NOT NULL,
    log_level VARCHAR(16) NOT NULL,
    category VARCHAR(32) NOT NULL,
    message TEXT,
    stack_trace TEXT,
    path VARCHAR(512),
    http_method VARCHAR(16),
    status_code INT,
    execution_time_ms BIGINT,
    user_id VARCHAR(128),
    user_email VARCHAR(128),
    metadata TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_log_trace_id ON log_entries(trace_id);
CREATE INDEX IF NOT EXISTS idx_log_service_name ON log_entries(service_name);
CREATE INDEX IF NOT EXISTS idx_log_level ON log_entries(log_level);
CREATE INDEX IF NOT EXISTS idx_log_timestamp ON log_entries(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_category ON log_entries(category);
