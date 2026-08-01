-- V5__add_dataset_columns_and_profiles.sql
-- Add dataset_columns and column_profiles tables for Dataset Inspection & Profiling

CREATE TABLE IF NOT EXISTS dataset_columns (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    data_type VARCHAR(255) NOT NULL,
    is_nullable BOOLEAN NOT NULL DEFAULT TRUE,
    is_primary_key BOOLEAN NOT NULL DEFAULT FALSE,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS column_profiles (
    id UUID PRIMARY KEY,
    null_count BIGINT NOT NULL DEFAULT 0,
    null_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    distinct_count BIGINT NOT NULL DEFAULT 0,
    min_value VARCHAR(255),
    max_value VARCHAR(255),
    avg_value DOUBLE PRECISION,
    profiled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    column_id UUID NOT NULL REFERENCES dataset_columns(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dataset_columns_dataset ON dataset_columns(dataset_id);
CREATE INDEX IF NOT EXISTS idx_column_profiles_column ON column_profiles(column_id);
