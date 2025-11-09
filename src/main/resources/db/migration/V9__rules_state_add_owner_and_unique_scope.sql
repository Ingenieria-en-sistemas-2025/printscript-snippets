DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name='rules_state'
      AND column_name='options_json'
      AND udt_name <> 'jsonb'
  ) THEN
    -- limpiar strings inválidos
UPDATE rules_state
SET options_json = NULL
WHERE options_json IS NOT NULL
  AND options_json !~ '^\s*\{.*\}\s*$';

ALTER TABLE rules_state
ALTER COLUMN options_json TYPE jsonb
      USING options_json::jsonb;
END IF;
END$$;
