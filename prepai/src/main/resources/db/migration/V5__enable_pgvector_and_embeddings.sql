-- RAG cache (spec 5.1, 6.3): problem and verified-solution embeddings.
-- The vector extension is created by a superuser (spec 2.5); IF NOT EXISTS keeps this migration a no-op there.
-- Timestamps are WITH TIME ZONE so they map to Instant without conversion (same choice as V1, V7 and V8).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE problem_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject VARCHAR(50) NOT NULL,
    topic VARCHAR(255),
    exam VARCHAR(50),
    problem_text TEXT NOT NULL,
    solution_json JSONB NOT NULL,
    embedding vector(384),
    numeric_signature VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    ask_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- HNSW instead of ivfflat: ivfflat needs representative data when the index is built, and this table starts empty
CREATE INDEX idx_problem_embeddings_vector ON problem_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_problem_embeddings_verified ON problem_embeddings(subject) WHERE verified = TRUE;
