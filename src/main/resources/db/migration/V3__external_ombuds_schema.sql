-- V3 - EXTERNAL OMBUDSMAN SCHEMA

-- 1. SAFETY CHECK

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users)
        OR EXISTS (SELECT 1 FROM reports) THEN

        RAISE EXCEPTION
            'V3 external migration requires an empty external database.';
END IF;
END
$$;

-- 2. USERS

ALTER TABLE users
DROP CONSTRAINT chk_users_role;

ALTER TABLE users
DROP CONSTRAINT uk_users_cpf;


ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NOT NULL;


ALTER TABLE users
    ADD CONSTRAINT uk_users_username
        UNIQUE (username);

ALTER TABLE users
    ADD CONSTRAINT chk_users_username_lowercase
        CHECK (username = LOWER(username));

ALTER TABLE users
    ADD CONSTRAINT chk_users_username_not_blank
        CHECK (LENGTH(BTRIM(username)) > 0);

ALTER TABLE users
    ADD CONSTRAINT chk_users_username_trimmed
        CHECK (username = BTRIM(username));

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('CLIENT', 'ADMIN'));


ALTER TABLE users
DROP COLUMN cpf;

-- 3. REPORTS

ALTER TABLE reports
DROP CONSTRAINT chk_reports_status;


ALTER TABLE reports
    ADD COLUMN owner_user_id UUID NOT NULL,
    ADD COLUMN closed_at TIMESTAMPTZ(6),
    ADD COLUMN messages_purged_at TIMESTAMPTZ(6);


ALTER TABLE reports
    ADD CONSTRAINT fk_reports_owner_user
        FOREIGN KEY (owner_user_id)
            REFERENCES users(id);


ALTER TABLE reports
    ADD CONSTRAINT chk_reports_purge_requires_closed
        CHECK (
            messages_purged_at IS NULL
                OR closed_at IS NOT NULL
            );


ALTER TABLE reports
DROP COLUMN access_code_hash,
    DROP COLUMN status;


CREATE INDEX idx_reports_owner_user_id
    ON reports(owner_user_id);

CREATE INDEX idx_reports_closed_at
    ON reports(closed_at)
    WHERE closed_at IS NOT NULL;

-- 4. REMOVE LEGACY STATUS HISTORY

DROP TABLE status_histories;

-- 5. REPORT MESSAGES

CREATE TABLE report_messages (
                                 id UUID PRIMARY KEY,
                                 report_id UUID NOT NULL,
                                 author_user_id UUID NOT NULL,
                                 body TEXT NOT NULL,
                                 created_at TIMESTAMPTZ(6) NOT NULL,

                                 CONSTRAINT fk_report_messages_report
                                     FOREIGN KEY (report_id)
                                         REFERENCES reports(id),

                                 CONSTRAINT fk_report_messages_author
                                     FOREIGN KEY (author_user_id)
                                         REFERENCES users(id),

                                 CONSTRAINT chk_report_messages_body_length
                                     CHECK (
                                         LENGTH(BTRIM(body)) > 0
                                             AND LENGTH(body) <= 10000
                                         )
);


CREATE INDEX idx_report_messages_report_created_id
    ON report_messages(report_id, created_at, id);

CREATE INDEX idx_report_messages_author_user_id
    ON report_messages(author_user_id);