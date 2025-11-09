-- Aseguro longitud y tipo de config_format (por si existían residuos)
ALTER TABLE rules_state
ALTER COLUMN config_format TYPE varchar(32);

-- Índices útiles para consultas por owner/tipo
CREATE INDEX IF NOT EXISTS idx_rules_state_owner_type
    ON rules_state(owner_id, type);

-- (Opcional) Si hacés lecturas frecuentes por type “GLOBAL”
CREATE INDEX IF NOT EXISTS idx_rules_state_type
    ON rules_state(type);