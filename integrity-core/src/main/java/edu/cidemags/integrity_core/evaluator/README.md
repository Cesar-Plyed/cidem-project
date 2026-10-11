# Módulo Evaluator

## Objetivo

El módulo evaluator gestiona la creación de exámenes, la generación de códigos de acceso, la participación de estudiantes y la revisión de respuestas abiertas. Está diseñado siguiendo una arquitectura por capas con dominio, casos de uso, puertos y adaptadores, similar al patrón ya utilizado en el módulo auth.

## Estructura principal

```text
src/main/java/edu/cidemags/integrity_core/evaluator/
├── application/
│   ├── dto/
│   └── usecase/
├── domain/
│   ├── exception/
│   ├── model/
│   └── port/
├── infrastructure/
│   ├── persistence/
│   └── web/
└── README.md
```

## Dominio

### Modelos principales

- Exam
  - Representa un examen.
  - Tiene título, descripción, creador, preguntas, duración en minutos y estado.
  - Estados: DRAFT, PUBLISHED, CLOSED.

- Question
  - Representa una pregunta.
  - Puede ser SINGLE_CHOICE, MULTIPLE_CHOICE u OPEN_ENDED.
  - Tiene opciones y respuesta abierta en caso de ser de texto libre.

- QuestionOption
  - Opción posible para una pregunta cerrada.
  - Incluye texto y flag de corrección.

- QuestionBank
  - Banco de preguntas reutilizable por tema o categoría.

- ExamAccessCode
  - Código temporal para acceder a un examen.
  - Tiene examen asociado, código generado y fecha de expiración.

- ExamAttempt
  - Intento de resolución del examen por un estudiante.
  - Guarda inicio, finalización y si se cerró por una infracción de integridad.

- ExamAnswer
  - Respuesta de una pregunta en el contexto del intento.
  - Puede incluir opción(es) seleccionadas o texto libre.

- EvaluationReview
  - Revisión manual de una respuesta abierta.
  - Guarda nota y feedback.

## Casos de uso

### Crear examen

- CreateExamUseCase
- Entrada: CreateExamCommand
- Salida: Exam

### Crear banco de preguntas

- CreateQuestionBankUseCase
- Entrada: CreateQuestionBankCommand
- Salida: QuestionBank

### Generar código de acceso

- GenerateExamAccessCodeUseCase
- Entrada: GenerateExamAccessCodeCommand
- Salida: ExamAccessCode

### Unirse al examen por código

- JoinExamByAccessCodeUseCase
- Valida que el examen exista, no esté cerrado y que el código siga siendo válido.
- Genera un nuevo ExamAttempt.

### Enviar intento

- SubmitExamAttemptUseCase
- Marca el intento como enviado.

### Guardar respuesta de una pregunta

- SubmitQuestionAnswerUseCase
- Guarda respuesta de opción múltiple o abierta.
- No permite enviar respuestas si el intento ya fue enviado.

### Revisar respuesta abierta

- ReviewOpenEndedAnswerUseCase
- Guarda nota y retroalimentación del docente.

## Puertos del dominio

Los puertos describen contratos que la capa de infraestructura debe implementar.

- ExamRepository
- QuestionBankRepository
- ExamAccessCodeRepository
- ExamAttemptRepository
- ExamAnswerRepository
- EvaluationReviewRepository

## Infraestructura

### Persistence

Se implementan entidades JPA para:

- ExamEntity
- QuestionEntity
- QuestionOptionEntity
- QuestionBankEntity
- ExamAccessCodeEntity
- ExamAttemptEntity
- ExamAnswerEntity
- EvaluationReviewEntity

Además existen adaptadores que convierten entre entidades JPA y modelos de dominio.

### Web

El módulo expone endpoints REST bajo el prefijo:

```text
/api/evaluator
```

## Endpoints actuales

### Exámenes

- POST /api/evaluator/exams
  - Crea un examen. El cuerpo incluye `durationMinutes` (mínimo 1).
- PUT /api/evaluator/exams/{examId}
  - Actualiza los datos del examen, incluida su duración.
- DELETE /api/evaluator/exams/{examId}
  - Elimina un examen en cualquier estado si ningún alumno ha iniciado un intento.
- GET /api/evaluator/exams/{examId}/results
  - Devuelve el intento más reciente de cada alumno para la vista del docente.

- POST /api/evaluator/exams/{examId}/access-code
  - Genera un código temporal para entrar al examen.

### Intentos

- POST /api/evaluator/attempts/join
  - El alumno se une al examen con un código.
- GET /api/evaluator/attempts/history
  - Devuelve al alumno sus intentos entregados, con acceso al resultado de cada uno.
- POST /api/evaluator/attempts/join y /api/evaluator/attempts/join-by-code
  - Solo permiten un intento por alumno y examen; la regla se vincula al `examId`, no al código temporal. Si ya existe un intento abierto, se devuelve ese mismo; tras entregar el examen, un nuevo ingreso responde 409.
- GET /api/evaluator/attempts/{attemptId}/result
  - Consulta el resultado del intento propio una vez entregado.
- POST /api/evaluator/attempts/{attemptId}/submit
  - Envía el examen terminado. Repetir la entrega del mismo intento ya enviado devuelve su estado actual.
- POST /api/evaluator/attempts/{attemptId}/answers
  - Guarda la respuesta a una pregunta del intento.
- POST /api/evaluator/attempts/{attemptId}/revoke
  - Permite al docente propietario o al administrador revocar el intento; lo cierra y lo califica con cero.

### Revisión

- POST /api/evaluator/reviews/open-ended
  - Guarda la corrección manual de una respuesta abierta.

## Flujo de uso recomendado

1. Crear un examen.
2. Crear un banco de preguntas o construir preguntas directamente.
3. Publicar el examen.
4. Generar código de acceso.
5. El estudiante entra con el código.
6. El alumno responde preguntas.
7. El alumno envía el intento.
8. El docente revisa respuestas abiertas; el resultado final se muestra al alumno al terminar esa revisión.

## Reglas de negocio actuales

- Un examen sin preguntas no puede publicarse.
- Las preguntas cerradas requieren opciones.
- Las preguntas abiertas no deben contener opciones.
- Los códigos de acceso deben ser válidos y no expirados.
- No se permite responder si el intento ya fue entregado.
- El plazo se calcula desde el inicio persistido del intento y no se reinicia al generar otro código de acceso.
- Si el índice de integridad baja de 50, el intento se cierra y queda reprobado automáticamente.
- Un examen con intentos de alumnos se conserva para proteger su historial y resultados.
- Cada alumno puede realizar un solo intento por examen, sin importar cuántos códigos de acceso se generen.

## Observaciones de diseño

- Se prioriza un enfoque de dominio puro con validaciones en el modelo.
- Las excepciones de dominio permiten controlar errores de negocio sin mezclarlos con errores de infraestructura.
- La capa de infraestructura encapsula JPA y Spring MVC.
- La lógica de acceso se deja en la capa de aplicación, no en los controladores.

## Estado actual

Las entidades y contratos están respaldados por migraciones Flyway. Las operaciones de gestión y revocación están protegidas por rol y verifican la propiedad del examen cuando corresponde; la consulta de historial y resultados del alumno se limita a sus propios intentos.

## Notas

Este módulo está pensado para crecer con el mismo patrón aplicado en auth: capas separadas, inyección por interfaces, y uso de Spring Data JPA para la persistencia.
