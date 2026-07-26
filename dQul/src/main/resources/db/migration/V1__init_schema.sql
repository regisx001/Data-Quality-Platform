-- V1__init_schema.sql
-- Initial schema for Data Quality Platform

CREATE TABLE IF NOT EXISTS datasources (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    registration_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS datasets (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_discovered TIMESTAMP,
    last_validated TIMESTAMP,
    domain VARCHAR(255),
    tags VARCHAR(255),
    criticality VARCHAR(50),
    datasource_id UUID NOT NULL REFERENCES datasources(id)
);

CREATE TABLE IF NOT EXISTS quality_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    expectation TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    target VARCHAR(255) NOT NULL,
    condition_expression TEXT,
    last_executed TIMESTAMP,
    dataset_id UUID NOT NULL REFERENCES datasets(id)
);

CREATE TABLE IF NOT EXISTS validations (
    id UUID PRIMARY KEY,
    trigger VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    total_rules BIGINT NOT NULL DEFAULT 0,
    passed_rules BIGINT NOT NULL DEFAULT 0,
    failed_rules BIGINT NOT NULL DEFAULT 0,
    total_findings BIGINT NOT NULL DEFAULT 0,
    dataset_id UUID NOT NULL REFERENCES datasets(id)
);

CREATE TABLE IF NOT EXISTS validation_rule_snapshots (
    id UUID PRIMARY KEY,
    quality_rule_id UUID NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_description TEXT,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    expectation TEXT NOT NULL,
    target VARCHAR(255) NOT NULL,
    validation_id UUID NOT NULL REFERENCES validations(id)
);

CREATE TABLE IF NOT EXISTS findings (
    id UUID PRIMARY KEY,
    description TEXT NOT NULL,
    details TEXT,
    severity VARCHAR(50) NOT NULL,
    affected_records BIGINT NOT NULL DEFAULT 0,
    detected_at TIMESTAMP NOT NULL,
    validation_id UUID NOT NULL REFERENCES validations(id),
    quality_rule_id UUID NOT NULL REFERENCES quality_rules(id)
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    error_message TEXT,
    recipient_id UUID NOT NULL REFERENCES users(id)
);

-- Indexes for foreign keys and common queries
CREATE INDEX IF NOT EXISTS idx_datasets_datasource ON datasets(datasource_id);
CREATE INDEX IF NOT EXISTS idx_quality_rules_dataset ON quality_rules(dataset_id);
CREATE INDEX IF NOT EXISTS idx_validations_dataset ON validations(dataset_id);
CREATE INDEX IF NOT EXISTS idx_validation_rule_snapshots_validation ON validation_rule_snapshots(validation_id);
CREATE INDEX IF NOT EXISTS idx_findings_validation ON findings(validation_id);
CREATE INDEX IF NOT EXISTS idx_findings_quality_rule ON findings(quality_rule_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id);
