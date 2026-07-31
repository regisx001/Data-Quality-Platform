-- V3__add_datasource_config_json.sql
-- Adds a config_json column to the datasources table for storing
-- per-datasource connector configuration (e.g. connection strings,
-- file paths, credentials) as a JSON string.

ALTER TABLE datasources
    ADD COLUMN IF NOT EXISTS config_json TEXT;
