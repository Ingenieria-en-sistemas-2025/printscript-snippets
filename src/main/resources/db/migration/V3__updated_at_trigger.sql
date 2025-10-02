CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_test_case_updated_at ON test_case;
CREATE TRIGGER trg_test_case_updated_at
    BEFORE UPDATE ON test_case
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
