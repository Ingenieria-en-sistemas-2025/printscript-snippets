BEGIN;

-- 1) quitar default de texto
ALTER TABLE rules_state
    ALTER COLUMN enabled_json DROP DEFAULT;

-- 2) convertir SIEMPRE a jsonb con USING que devuelve jsonb en todas las ramas
ALTER TABLE rules_state
ALTER COLUMN enabled_json TYPE jsonb
  USING
    CASE
      WHEN enabled_json IS NULL THEN '[]'::jsonb
      ELSE enabled_json::jsonb
END;

-- 3) normalizar nulos
UPDATE rules_state SET enabled_json = '[]'::jsonb WHERE enabled_json IS NULL;

-- 4) default y not null finales
ALTER TABLE rules_state
    ALTER COLUMN enabled_json SET DEFAULT '[]'::jsonb,
ALTER COLUMN enabled_json SET NOT NULL;

COMMIT;
