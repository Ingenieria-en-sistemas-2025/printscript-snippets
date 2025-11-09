CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE snippet (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         owner_id VARCHAR(120) NOT NULL,
                         name VARCHAR(120) NOT NULL,
                         description TEXT,
                         language VARCHAR(40) NOT NULL,
                         language_version VARCHAR(10) NOT NULL,
                         current_version_id UUID,
                         last_is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                         last_lint_count INTEGER NOT NULL DEFAULT 0,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         CONSTRAINT uq_owner_name UNIQUE (owner_id, name)
);

CREATE INDEX ix_snippet_owner         ON snippet(owner_id);
CREATE INDEX ix_snippet_name          ON snippet(name);
CREATE INDEX ix_snippet_language      ON snippet(language);
CREATE INDEX ix_snippet_last_is_valid ON snippet(last_is_valid);

CREATE TABLE snippet_version (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 snippet_id UUID NOT NULL REFERENCES snippet(id) ON DELETE CASCADE,
                                 version_number BIGINT NOT NULL,
                                 content_key VARCHAR(512) NOT NULL,
                                 formatted_key VARCHAR(512),
                                 is_formatted BOOLEAN NOT NULL DEFAULT FALSE,
                                 is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                                 lint_issues JSONB NOT NULL DEFAULT '[]'::jsonb,
                                 parse_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_sn_ver UNIQUE (snippet_id, version_number)
);

CREATE INDEX ix_snipver_snip_created ON snippet_version(snippet_id, created_at DESC);

ALTER TABLE snippet
    ADD CONSTRAINT fk_snip_current_ver
        FOREIGN KEY (current_version_id)
            REFERENCES snippet_version(id)
            DEFERRABLE INITIALLY DEFERRED;
