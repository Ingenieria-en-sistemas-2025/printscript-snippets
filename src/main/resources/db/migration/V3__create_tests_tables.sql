CREATE TABLE test_case (
                           id BIGSERIAL PRIMARY KEY,
                           snippet_id BIGINT NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
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

CREATE INDEX ix_test_case_snippet     ON test_case(snippet_id);
CREATE INDEX ix_test_case_last_run_at ON test_case(last_run_at);


CREATE TABLE test_run (
                          id BIGSERIAL PRIMARY KEY,
                          test_id BIGINT NOT NULL REFERENCES test_case(id) ON DELETE CASCADE,
                          snippet_id BIGINT NOT NULL,
                          snippet_version_id BIGINT NOT NULL REFERENCES snippet_version(id) ON DELETE CASCADE,
                          status VARCHAR(20) NOT NULL,
                          inputs JSONB NOT NULL,
                          expected_outputs JSONB NOT NULL,
                          outputs JSONB,
                          error_message TEXT,
                          duration_ms BIGINT,
                          executed_by VARCHAR(120),
                          executed_at TIMESTAMP NOT NULL DEFAULT now(),
                          run_type VARCHAR(20) DEFAULT 'MANUAL' CHECK (run_type IN ('MANUAL','AUTOMATIC')),
                          CHECK (jsonb_typeof(inputs) = 'array'),
                          CHECK (jsonb_typeof(expected_outputs) = 'array'),
                          CHECK (status IN ('PASSED','FAILED','ERROR','TIMEOUT'))
);

CREATE INDEX IF NOT EXISTS ix_test_run_test        ON test_run(test_id);
CREATE INDEX IF NOT EXISTS ix_test_run_snippet_ver ON test_run(snippet_id, snippet_version_id);
CREATE INDEX IF NOT EXISTS ix_test_run_executed_at ON test_run(executed_at);