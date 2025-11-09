ALTER TABLE rules_state
    ADD COLUMN IF NOT EXISTS owner_id varchar(64);

-- eliminar cualquier UNIQUE previa sobre (type) sola
ALTER TABLE rules_state DROP CONSTRAINT IF EXISTS uq_rules_scope;
ALTER TABLE rules_state DROP CONSTRAINT IF EXISTS rules_state_type_key;

DO $$
DECLARE c RECORD;
BEGIN
FOR c IN
SELECT conname
FROM   pg_constraint
WHERE  conrelid = 'rules_state'::regclass
       AND contype = 'u'
       AND conkey = ARRAY[
         (SELECT attnum FROM pg_attribute
          WHERE attrelid='rules_state'::regclass AND attname='type')
       ]
  LOOP
    EXECUTE format('ALTER TABLE rules_state DROP CONSTRAINT %I', c.conname);
END LOOP;
END$$;

ALTER TABLE rules_state
    ADD CONSTRAINT uq_rules_scope UNIQUE (type, owner_id);
