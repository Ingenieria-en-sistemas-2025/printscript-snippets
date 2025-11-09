ALTER TABLE rules_state
    ADD COLUMN IF NOT EXISTS owner_id varchar(64);

-- 2) Drop de cualquier unique previa por (type) o con nombres viejos
ALTER TABLE rules_state DROP CONSTRAINT IF EXISTS uq_rules_scope;
ALTER TABLE rules_state DROP CONSTRAINT IF EXISTS rules_state_type_key;

-- 2.b) Por si Hibernate alguna vez creó una UNIQUE “anónima” sobre type
DO $$
DECLARE
c RECORD;
BEGIN
FOR c IN
SELECT conname
FROM   pg_constraint
WHERE  conrelid = 'rules_state'::regclass
      AND  contype = 'u'
      AND  conkey = ARRAY[
            (SELECT attnum FROM pg_attribute
             WHERE attrelid = 'rules_state'::regclass AND attname='type')
          ]
  LOOP
    EXECUTE format('ALTER TABLE rules_state DROP CONSTRAINT %I', c.conname);
END LOOP;
END$$;

-- 3) Creo la UNIQUE compuesta (type, owner_id)
ALTER TABLE rules_state
    ADD CONSTRAINT uq_rules_scope UNIQUE (type, owner_id);

