-- V2__add_user_verified.sql
-- Add the verified column to users table

ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT FALSE;
