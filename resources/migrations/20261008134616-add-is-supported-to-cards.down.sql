-- Migration: add-is-supported-to-cards (DOWN)
ALTER TABLE cards DROP COLUMN is_supported;
