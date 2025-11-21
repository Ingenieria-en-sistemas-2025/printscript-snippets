DROP INDEX IF EXISTS ix_snipver_snip_created;

ALTER TABLE snippet_version
DROP COLUMN IF EXISTS parse_errors,
    DROP COLUMN IF EXISTS created_at;