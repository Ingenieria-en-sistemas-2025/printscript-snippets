CREATE TABLE test_case (
                           id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           snippet_id            UUID NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
                           name                  VARCHAR(120) NOT NULL,
                           inputs                JSONB NOT NULL DEFAULT '[]'::jsonb,
                           expected_outputs      JSONB NOT NULL DEFAULT '[]'::jsonb,
                           target_version_number BIGINT,
                           last_run_status       VARCHAR(20) NOT NULL DEFAULT 'NEVER_RUN' CHECK (last_run_status IN ('PASSED','FAILED','ERROR','NEVER_RUN')),
                           last_run_output       JSONB,
                           last_run_at           TIMESTAMPTZ,
                           created_by            VARCHAR(120) NOT NULL,
                           created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                           CHECK (jsonb_typeof(inputs) = 'array'),
                           CHECK (jsonb_typeof(expected_outputs) = 'array')
);

CREATE INDEX ix_test_case_snippet     ON test_case(snippet_id);
CREATE INDEX ix_test_case_last_run_at ON test_case(last_run_at);