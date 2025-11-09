-- V8: forzar enabled_json a jsonb sin default colgando

BEGIN;

-- 1) Quitar DEFAULT para poder castear el tipo
ALTER TABLE rules_state
    ALTER COLUMN enabled_json DROP DEFAULT;

-- 2) Castear a jsonb contemplando orígenes text/json/jsonb
--    - Si ya es jsonb, queda igual
--    - Si es json: lo castea a jsonb
--    - Si es text: lo interpreta como json y lo pasa a jsonb
ALTER TABLE rules_state
ALTER COLUMN enabled_json TYPE jsonb
  USING
    CASE
      WHEN enabled_json IS NULL THEN '[]'::jsonb
      WHEN pg_typeof(enabled_json)::text = 'jsonb' THEN enabled_json
      WHEN pg_typeof(enabled_json)::text = 'json'  THEN (enabled_json::jsonb)
      ELSE (enabled_json::jsonb)  -- text -> jsonb (si el contenido es JSON válido)
END;

-- 3) Normalizar nulos
UPDATE rules_state
SET enabled_json = '[]'::jsonb
WHERE enabled_json IS NULL;

-- 4) Volver a poner DEFAULT y NOT NULL
ALTER TABLE rules_state
    ALTER COLUMN enabled_json SET DEFAULT '[]'::jsonb,
ALTER COLUMN enabled_json SET NOT NULL;

COMMIT;