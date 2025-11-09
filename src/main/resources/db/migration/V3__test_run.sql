CREATE TABLE test_run (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          test_id UUID NOT NULL REFERENCES test_case(id) ON DELETE CASCADE,
                          snippet_id UUID NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
                          snippet_version_id UUID NOT NULL REFERENCES snippet_version(id) ON DELETE CASCADE,
                          status VARCHAR(20) NOT NULL CHECK (status IN ('PASSED','FAILED','ERROR','TIMEOUT')),
                          inputs JSONB NOT NULL,
                          expected_outputs JSONB NOT NULL,
                          outputs JSONB,
                          error_message TEXT,
                          duration_ms BIGINT,
                          executed_by VARCHAR(120),
                          executed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          run_type VARCHAR(20) DEFAULT 'MANUAL' CHECK (run_type IN ('MANUAL','AUTOMATIC')),
                          CHECK (jsonb_typeof(inputs) = 'array'),
                          CHECK (jsonb_typeof(expected_outputs) = 'array')
);

CREATE INDEX ix_test_run_test        ON test_run(test_id);
CREATE INDEX ix_test_run_snippet_ver ON test_run(snippet_id, snippet_version_id);
CREATE INDEX ix_test_run_executed_at ON test_run(executed_at);
