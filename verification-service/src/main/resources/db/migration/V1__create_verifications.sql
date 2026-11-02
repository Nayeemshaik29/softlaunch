CREATE TABLE verifications (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL,
    method         VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    subject_hash   VARCHAR(64),
    verified_name  VARCHAR(150),
    verified_dob   DATE,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_method CHECK (method IN ('DIGILOCKER')),
    CONSTRAINT chk_status CHECK (status IN ('VERIFIED', 'FAILED'))
);

CREATE INDEX idx_verifications_user ON verifications (user_id, created_at DESC);

-- one verified record per user
CREATE UNIQUE INDEX ux_verified_user    ON verifications (user_id)      WHERE status = 'VERIFIED';
-- one real person (DigiLocker identity) -> one SoftLaunch account
CREATE UNIQUE INDEX ux_verified_subject ON verifications (subject_hash) WHERE status = 'VERIFIED';
