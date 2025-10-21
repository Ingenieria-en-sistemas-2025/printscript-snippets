CREATE TABLE IF NOT EXISTS test_case (
                                         id BIGSERIAL PRIMARY KEY,
                                         snippet_id BIGINT NOT NULL,
                                         name VARCHAR(120) NOT NULL,
    inputs JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_outputs JSONB NOT NULL DEFAULT '[]'::jsonb,
    target_version_number BIGINT,
    last_run_status VARCHAR(20) NOT NULL DEFAULT 'NEVER_RUN',
    last_run_output JSONB,
    last_run_at TIMESTAMP,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (jsonb_typeof(inputs) = 'array'),
    CHECK (jsonb_typeof(expected_outputs) = 'array'),
    CHECK (last_run_status IN ('PASSED','FAILED','ERROR','NEVER_RUN'))
    );

CREATE INDEX IF NOT EXISTS ix_test_case_snippet      ON test_case(snippet_id);
CREATE INDEX IF NOT EXISTS ix_test_case_last_run_at  ON test_case(last_run_at);