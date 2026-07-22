-- Add email verification fields to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN pending_farm_name VARCHAR(100) NULL;
