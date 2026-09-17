ALTER TABLE question_banks
    ADD COLUMN created_by_user_id UUID REFERENCES users(id);

CREATE INDEX idx_question_banks_created_by_user ON question_banks(created_by_user_id);