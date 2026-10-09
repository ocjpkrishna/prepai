-- Usage log (spec 5.1): one row per lesson session the student really received.
-- lesson_id has no foreign key yet: the lessons table is created by row 13, which adds the constraint.
CREATE TABLE usage_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    lesson_id UUID,
    session_date DATE NOT NULL,
    duration_seconds INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_usage_log_user_date ON usage_log(user_id, session_date);
