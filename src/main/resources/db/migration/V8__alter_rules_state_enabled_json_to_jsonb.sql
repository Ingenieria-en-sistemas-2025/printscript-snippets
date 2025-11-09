-- enabled_json -> jsonb, not null, default []
ALTER TABLE rules_state
ALTER COLUMN enabled_json TYPE jsonb
    USING CASE
      WHEN enabled_json IS NULL OR enabled_json = '' THEN '[]'::jsonb
      ELSE enabled_json::jsonb
END;

ALTER TABLE rules_state
    ALTER COLUMN enabled_json SET DEFAULT '[]'::jsonb;

ALTER TABLE rules_state
    ALTER COLUMN enabled_json SET NOT NULL;

-- options_json -> jsonb (nullable)
ALTER TABLE rules_state
ALTER COLUMN options_json TYPE jsonb
    USING CASE
      WHEN options_json IS NULL OR options_json = '' THEN NULL
      ELSE options_json::jsonb
END;

