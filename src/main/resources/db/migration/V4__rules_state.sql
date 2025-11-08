CREATE TABLE IF NOT EXISTS rules_state (
                                           id             BIGSERIAL PRIMARY KEY,
                                           type           VARCHAR(16)  NOT NULL UNIQUE,   -- 'FORMAT' | 'LINT' (se guarda como string del enum)
    enabled_json   TEXT         NOT NULL DEFAULT '[]',
    options_json   TEXT,
    config_text    TEXT,
    config_format  VARCHAR(32)
    );


INSERT INTO rules_state (type, enabled_json, options_json, config_text, config_format)
VALUES
    (
        'FORMAT',
        '["indent-spaces","mandatory-single-space-separation","if-brace-same-line"]',
        '{"indent-spaces":3,"indent_size":3,"tabsize":3,"line-breaks-after-println":0,"line_breaks_after_println":0}',
        NULL,
        NULL
    ),
    (
        'LINT',
        '["IdentifierStyleRuleStreaming","PrintlnSimpleArgRuleStreaming"]',
        NULL,
        NULL,
        NULL
    )
    ON CONFLICT (type) DO NOTHING;