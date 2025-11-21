ALTER TABLE rules_state
    ADD COLUMN id_uuid uuid;

-- rellenar filas existentes
UPDATE rules_state
SET id_uuid = gen_random_uuid()
WHERE id_uuid IS NULL;

--borro la pk anterior
ALTER TABLE rules_state
DROP CONSTRAINT IF EXISTS rules_state_pkey;

--nueva pk
ALTER TABLE rules_state
    ADD CONSTRAINT rules_state_pkey PRIMARY KEY (id_uuid);

-- borro col vieja
ALTER TABLE rules_state
DROP COLUMN id;

-- renombro col nueva id
ALTER TABLE rules_state
    RENAME COLUMN id_uuid TO id;