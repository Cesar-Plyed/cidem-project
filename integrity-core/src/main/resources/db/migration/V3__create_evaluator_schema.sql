CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
);

CREATE INDEX idx_exams_created_by_user ON exams(created_by_user_id);
CREATE INDEX idx_exams_status ON exams(status);

CREATE TABLE question_banks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000)
);

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID REFERENCES exams(id) ON DELETE CASCADE,
    question_bank_id UUID REFERENCES question_banks(id) ON DELETE CASCADE,
    prompt VARCHAR(1000) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'OPEN_ENDED')),
    correct_answer_text VARCHAR(1000),
    CONSTRAINT ck_question_owner CHECK (exam_id IS NOT NULL OR question_bank_id IS NOT NULL)
);

CREATE INDEX idx_questions_exam ON questions(exam_id);
CREATE INDEX idx_questions_bank ON questions(question_bank_id);

CREATE TABLE question_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE exam_access_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_exam_access_code UNIQUE (exam_id, code)
);

CREATE INDEX idx_exam_access_codes_lookup ON exam_access_codes(exam_id, code);

CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    submitted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_exam_attempts_exam ON exam_attempts(exam_id);
CREATE INDEX idx_exam_attempts_student ON exam_attempts(student_id);

CREATE TABLE exam_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id),
    open_text_answer VARCHAR(4000),
    CONSTRAINT uq_exam_answer_question UNIQUE (attempt_id, question_id)
);

CREATE TABLE exam_answer_selected_options (
    answer_id UUID NOT NULL REFERENCES exam_answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES question_options(id),
    PRIMARY KEY (answer_id, option_id)
);

CREATE TABLE evaluation_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL REFERENCES exam_answers(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    score INTEGER NOT NULL CHECK (score >= 0),
    feedback VARCHAR(2000),
    CONSTRAINT uq_evaluation_review_answer UNIQUE (answer_id)
);