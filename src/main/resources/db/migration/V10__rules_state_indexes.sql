CREATE INDEX IF NOT EXISTS idx_rules_state_owner_type ON rules_state(owner_id, type);
CREATE INDEX IF NOT EXISTS idx_rules_state_type       ON rules_state(type);
