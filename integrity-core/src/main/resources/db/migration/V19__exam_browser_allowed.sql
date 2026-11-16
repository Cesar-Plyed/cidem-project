-- Spec 51: control de plataforma por examen.
-- browser_allowed=false (default) exige la app de escritorio para iniciar el intento;
-- el docente puede permitir navegador al crear/editar el examen.
ALTER TABLE exams ADD COLUMN browser_allowed BOOLEAN NOT NULL DEFAULT false;
