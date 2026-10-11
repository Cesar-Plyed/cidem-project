CREATE TABLE exam_student_participation (
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (exam_id, student_id)
);

INSERT INTO exam_student_participation (exam_id, student_id)
SELECT DISTINCT exam_id, student_id
FROM exam_attempts
ON CONFLICT (exam_id, student_id) DO NOTHING;
