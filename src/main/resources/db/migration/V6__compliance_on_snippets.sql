ALTER TABLE snippet
    ADD COLUMN compliance VARCHAR(32) NOT NULL DEFAULT 'pending';