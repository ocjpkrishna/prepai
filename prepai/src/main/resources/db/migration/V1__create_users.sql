-- Users (spec 5.1). Timestamps are TIMESTAMP WITH TIME ZONE so that Instant maps without conversion (see BUILD-DECISIONS.md).
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    name VARCHAR(255),
    google_id VARCHAR(255),
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    plan_expires_at TIMESTAMP WITH TIME ZONE,
    language VARCHAR(5) NOT NULL DEFAULT 'EN',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_minor BOOLEAN NOT NULL DEFAULT FALSE,
    guardian_email VARCHAR(255),
    guardian_consent_at TIMESTAMP WITH TIME ZONE,
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    privacy_policy_version VARCHAR(20),
    deletion_requested_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
