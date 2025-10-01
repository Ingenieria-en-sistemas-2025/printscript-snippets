CREATE TABLE test_case (
                           id BIGSERIAL PRIMARY KEY,
                           snippet_id BIGINT NOT NULL,
                           name VARCHAR(120) NOT NULL,
                           inputs JSONB,
                           expected_outputs JSONB,
                           created_at TIMESTAMP NOT NULL DEFAULT now()
);

