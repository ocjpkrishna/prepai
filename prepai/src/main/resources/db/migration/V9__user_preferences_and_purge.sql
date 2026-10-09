-- Preferences (spec 4.3) and the purge marker for deleted accounts (spec 2.7). The consent columns already exist in V1.
ALTER TABLE users
    ADD COLUMN voice_speed DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'light',
    ADD COLUMN purged_at TIMESTAMP WITH TIME ZONE;
