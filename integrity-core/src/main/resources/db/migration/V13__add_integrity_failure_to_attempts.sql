ALTER TABLE exam_attempts
    ADD COLUMN integrity_failed BOOLEAN NOT NULL DEFAULT FALSE;
