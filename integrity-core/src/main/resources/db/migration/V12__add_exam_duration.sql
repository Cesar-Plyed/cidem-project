ALTER TABLE exams
    ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 30
    CHECK (duration_minutes >= 1);
