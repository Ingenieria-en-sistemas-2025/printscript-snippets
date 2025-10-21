CREATE TABLE snippet (
                         id BIGSERIAL PRIMARY KEY,
                         owner_id VARCHAR(120) NOT NULL,
                         name VARCHAR(120) NOT NULL,
                         description TEXT,
                         language VARCHAR(40) NOT NULL,
                         version VARCHAR(10) NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE snippet_version (
                                 id BIGSERIAL PRIMARY KEY,
                                 snippet_id BIGINT NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
                                 content TEXT NOT NULL,
                                 is_formatted BOOLEAN NOT NULL DEFAULT FALSE,
                                 is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                                 lint_issues JSONB DEFAULT '[]'::jsonb,
                                 parse_errors JSONB DEFAULT '[]'::jsonb,
                                 created_at TIMESTAMP NOT NULL DEFAULT now()
);
