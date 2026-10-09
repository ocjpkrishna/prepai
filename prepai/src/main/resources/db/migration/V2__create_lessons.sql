-- Lessons (spec 5.1): one row per lesson a student received, with the validated response as JSONB.
-- Timestamps are WITH TIME ZONE like the other tables. `title` is added to the spec 5.1 columns so history can list it.
CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subject VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    topic VARCHAR(255),
    exam VARCHAR(50),
    difficulty VARCHAR(20),
    input_text TEXT,
    response_json JSONB NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'LLM',
    llm_provider VARCHAR(50),
    llm_model VARCHAR(100),
    retried BOOLEAN NOT NULL DEFAULT FALSE,
    retry_reason VARCHAR(30),
    validation_attempts SMALLINT NOT NULL DEFAULT 1,
    generation_ms INTEGER,
    token_count INTEGER,
    estimated_cost_usd DECIMAL(10, 6),
    duration_seconds INTEGER,
    rating SMALLINT,
    feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lessons_user_created ON lessons(user_id, created_at DESC);
