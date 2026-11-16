-- =====================================================================
-- Spec 51 — Limpieza de exámenes de prueba en PRODUCCIÓN
--
-- Criterio: exámenes creados por cuentas de prueba (emails que contienen
-- "test" o dominio test.*, p. ej. <algo>test@gmail.com, <algo>@test.com).
--
-- Las dependencias (questions, question_options, exam_access_codes,
-- exam_attempts, exam_answers, exam_answer_selected_options,
-- evaluation_reviews, safeguard_configuration, safeguard_session,
-- exam_student_participation, exam_session) tienen
-- `ON DELETE CASCADE` hacia exams, así que basta con borrar los exámenes.
--
-- USO: ejecuta TODO el bloque en una transacción y revisa el SELECT de
-- verificación antes de descomentar el DELETE. Los DELETE van comentados
-- a propósito: quítale los comentarios solo tras revisar el listado.
-- =====================================================================

BEGIN;

-- 1) Verificación: qué se va a borrar (revisar esta lista ANTES de borrar).
SELECT e.id, e.title, e.status, e.created_at, u.email
FROM exams e
JOIN users u ON u.id = e.created_by_user_id
WHERE lower(u.email) LIKE '%test%'
   OR lower(u.email) LIKE '%@test.%';

-- 2) Dependientes (cuántas filas se irán por CASCADE).
SELECT
  (SELECT count(*) FROM exam_attempts a JOIN exams e ON e.id = a.exam_id
     JOIN users u ON u.id = e.created_by_user_id
     WHERE lower(u.email) LIKE '%test%' OR lower(u.email) LIKE '%@test.%') AS attempts,
  (SELECT count(*) FROM questions q JOIN exams e ON e.id = q.exam_id
     JOIN users u ON u.id = e.created_by_user_id
     WHERE lower(u.email) LIKE '%test%' OR lower(u.email) LIKE '%@test.%') AS questions,
  (SELECT count(*) FROM safeguard_configuration sc JOIN exams e ON e.id = sc.exam_id
     JOIN users u ON u.id = e.created_by_user_id
     WHERE lower(u.email) LIKE '%test%' OR lower(u.email) LIKE '%@test.%') AS safeguard_configs;

-- 3) Borrado (descomentar solo tras revisar el SELECT anterior).
-- DELETE FROM exams e
-- USING users u
-- WHERE e.created_by_user_id = u.id
--   AND (lower(u.email) LIKE '%test%' OR lower(u.email) LIKE '%@test.%');

-- 4) Si todo cuadra: COMMIT; si algo no cuadra: ROLLBACK;
-- COMMIT;
-- ROLLBACK;
