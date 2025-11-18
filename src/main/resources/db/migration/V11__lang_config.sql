CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE language_config (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 language VARCHAR(255) NOT NULL,
                                 extension VARCHAR(50) NOT NULL,
                                 version VARCHAR(50) NOT NULL,
                                 CONSTRAINT uq_language_version UNIQUE (language, version)
);

INSERT INTO language_config (id, language, extension, version)
VALUES
    (gen_random_uuid(), 'printscript', 'prs', '1.0'),
    (gen_random_uuid(), 'printscript', 'prs', '1.1');