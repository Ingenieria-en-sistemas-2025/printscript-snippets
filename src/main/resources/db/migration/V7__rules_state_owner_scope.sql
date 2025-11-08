ALTER TABLE rules_state ADD COLUMN owner_id varchar(64);
ALTER TABLE rules_state DROP CONSTRAINT IF EXISTS uq_rules_scope;
ALTER TABLE rules_state ADD CONSTRAINT uq_rules_scope UNIQUE (type, owner_id);
-- Las filas existentes pasan a GLOBAL
UPDATE rules_state SET owner_id = NULL WHERE owner_id IS NULL;