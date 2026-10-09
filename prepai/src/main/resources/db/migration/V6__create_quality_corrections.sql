-- Quality: nightly verifier corrections log (spec 5.2, 8.1).
-- lesson_id has no foreign key yet: `lessons` is created by V2 in row 13, which adds the constraint (same as usage_log).
-- Timestamps are WITH TIME ZONE so they map to Instant without conversion (same choice as V1, V5, V7 and V8).
CREATE TABLE quality_corrections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID,
    problem_text TEXT NOT NULL,
    original_answer JSONB NOT NULL,
    corrected_answer JSONB NOT NULL,
    error_type VARCHAR(100),
    verifier_model VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_quality_corrections_error_type ON quality_corrections(error_type);
