-- Normalizo valores nulos en enabled_json antes de convertir
UPDATE rules_state
SET enabled_json = '[]'
WHERE enabled_json IS NULL;

-- Aseguro que enabled_json parezca JSON válido (defensivo)
UPDATE rules_state
SET enabled_json = '[]'
WHERE enabled_json IS NOT NULL
  AND enabled_json !~ '^\s*(\{.*\}|\[.*\])\s*$';

-- 1) Remuevo DEFAULT viejo (si existía como text)
ALTER TABLE rules_state
    ALTER COLUMN enabled_json DROP DEFAULT;

-- 2) Convierto enabled_json a JSONB con cast explícito
ALTER TABLE rules_state
ALTER COLUMN enabled_json TYPE jsonb
  USING enabled_json::jsonb;

-- 3) Seteo DEFAULT nuevo como jsonb y NOT NULL (tu entidad lo requiere)
ALTER TABLE rules_state
    ALTER COLUMN enabled_json SET DEFAULT '[]'::jsonb,
ALTER COLUMN enabled_json SET NOT NULL;

-- 4) Convierto options_json a jsonb si todavía no lo es.
--    Si la columna ya es jsonb, este ALTER no hace nada.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name='rules_state' AND column_name='options_json' AND udt_name <> 'jsonb'
  ) THEN
    -- Si tenías strings “sucios”, los paso a NULL
UPDATE rules_state
SET options_json = NULL
WHERE options_json IS NOT NULL
  AND options_json !~ '^\s*\{.*\}\s*$';

ALTER TABLE rules_state
ALTER COLUMN options_json TYPE jsonb
      USING options_json::jsonb;
END IF;
END$$;

-- No seteamos default para options_json (queda NULL por diseño)