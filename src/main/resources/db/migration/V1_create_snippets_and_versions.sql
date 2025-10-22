CREATE TABLE snippet (
                         id BIGSERIAL PRIMARY KEY,
                         owner_id VARCHAR(120) NOT NULL,
                         name VARCHAR(120) NOT NULL,
                         description TEXT,
                         language VARCHAR(40) NOT NULL,
                         version VARCHAR(10) NOT NULL,
                         current_version_id BIGINT, -- aún sin FK
                         last_is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                         last_lint_count INTEGER NOT NULL DEFAULT 0,
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP NOT NULL DEFAULT now(),
                         CONSTRAINT uq_owner_name UNIQUE (owner_id, name)
);

CREATE INDEX ix_snippet_owner    ON snippet(owner_id);
CREATE INDEX ix_snippet_name     ON snippet(name);
CREATE INDEX ix_snippet_language ON snippet(language);
CREATE INDEX ix_snippet_is_valid ON snippet(last_is_valid);


CREATE TABLE snippet_version (
                                 id BIGSERIAL PRIMARY KEY,
                                 snippet_id BIGINT NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
                                 version_number BIGINT NOT NULL,
                                 content TEXT NOT NULL,
                                 is_formatted BOOLEAN NOT NULL DEFAULT FALSE,
                                 is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                                 lint_issues JSONB DEFAULT '[]'::jsonb,
                                 parse_errors JSONB DEFAULT '[]'::jsonb,
                                 created_at TIMESTAMP NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_sn_ver UNIQUE (snippet_id, version_number)
);

CREATE INDEX ix_snipver_snip_created ON snippet_version(snippet_id, created_at DESC);