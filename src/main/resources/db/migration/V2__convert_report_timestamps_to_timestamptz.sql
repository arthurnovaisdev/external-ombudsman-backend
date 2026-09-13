ALTER TABLE reports
    ALTER COLUMN created_at TYPE TIMESTAMPTZ(6)
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ(6)
        USING updated_at AT TIME ZONE 'UTC';
