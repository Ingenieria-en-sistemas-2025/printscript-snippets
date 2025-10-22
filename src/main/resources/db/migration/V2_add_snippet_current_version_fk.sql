ALTER TABLE snippet
    ADD CONSTRAINT fk_snip_current_ver
        FOREIGN KEY (current_version_id)
            REFERENCES snippet_version(id)
            DEFERRABLE INITIALLY DEFERRED;