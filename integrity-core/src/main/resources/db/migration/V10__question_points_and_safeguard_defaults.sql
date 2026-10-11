-- Escala de calificacion: cada pregunta vale puntos (1..10) y la suma del
-- examen no puede exceder 10 (validado en la capa de dominio, no aqui, porque
-- es un constraint entre filas). Las preguntas existentes se reparten los 10
-- puntos del examen (minimo 1 por pregunta).
ALTER TABLE questions ADD COLUMN points INTEGER NOT NULL DEFAULT 1;
ALTER TABLE questions ADD CONSTRAINT ck_question_points CHECK (points BETWEEN 1 AND 10);

UPDATE questions q
SET points = GREATEST(1, (10 / sub.total_questions)::int)
FROM (
    SELECT exam_id, COUNT(*) AS total_questions
    FROM questions
    WHERE exam_id IS NOT NULL
    GROUP BY exam_id
) sub
WHERE q.exam_id = sub.exam_id;

-- Una revision no puede superar los puntos de su pregunta: los datos previos a
-- esta migracion admitian cualquier puntaje y rompian la escala (p. ej. 10/2).
UPDATE evaluation_reviews er
SET score = q.points
FROM exam_answers ea
    JOIN questions q ON q.id = ea.question_id
WHERE er.answer_id = ea.id
  AND er.score > q.points;

-- SafeGuard quedaba inactivo en todo examen creado antes de existir UI de
-- configuracion: se siembra la configuracion por defecto (proteccion activa,
-- pantalla completa obligatoria y bloqueo de copiar/pegar) para los examenes
-- existentes. Los examenes nuevos la reciben al crearse (ExamCreatedEvent).
INSERT INTO safeguard_configuration (id, exam_id, enabled, require_fullscreen, block_copy_paste)
SELECT gen_random_uuid(), e.id, TRUE, TRUE, TRUE
FROM exams e
WHERE NOT EXISTS (SELECT 1 FROM safeguard_configuration sc WHERE sc.exam_id = e.id);
