CREATE TABLE test_run (
                          id BIGSERIAL PRIMARY KEY,
                          test_id BIGINT NOT NULL REFERENCES test_case(id) ON DELETE CASCADE,
                          snippet_id BIGINT NOT NULL,
                          snippet_version_number BIGINT NOT NULL,

                          status VARCHAR(20) NOT NULL, -- PASSED | FAILED | ERROR | TIMEOUT
                          inputs JSONB NOT NULL,
                          expected_outputs JSONB NOT NULL,
                          outputs JSONB,
                          error_message TEXT,
                          duration_ms BIGINT,
                          executed_by VARCHAR(120),
                          executed_at TIMESTAMP NOT NULL DEFAULT now(),

                          CHECK (jsonb_typeof(inputs) = 'array'),
                          CHECK (jsonb_typeof(expected_outputs) = 'array'),
                          CHECK (status IN ('PASSED','FAILED','ERROR','TIMEOUT'))
);

CREATE INDEX ix_test_run_test        ON test_run(test_id);
CREATE INDEX ix_test_run_snippet_ver ON test_run(snippet_id, snippet_version_number);
CREATE INDEX ix_test_run_executed_at ON test_run(executed_at);
