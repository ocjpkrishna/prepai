-- Subscriptions (spec 5.1) and the Razorpay webhook events already handled (idempotency).
-- Timestamps are WITH TIME ZONE, like V1, V3 and V7 to V9.
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    plan VARCHAR(20) NOT NULL,
    razorpay_subscription_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE UNIQUE INDEX idx_subscriptions_razorpay ON subscriptions(razorpay_subscription_id);

CREATE TABLE processed_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
