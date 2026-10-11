package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ExamAverageResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.PagedExamResultsResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.StudentExamHistoryResponse;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.EvaluationReview;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAnswer;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import edu.cidemags.integrity_core.evaluator.domain.port.EvaluationReviewRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAnswerRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import edu.cidemags.integrity_core.auth.domain.model.Phone;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class GetExamResultUseCase {
    private final ExamAttemptRepository attemptRepository;
    private final ExamRepository examRepository;
    private final ExamAnswerRepository answerRepository;
    private final EvaluationReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public GetExamResultUseCase(ExamAttemptRepository attemptRepository, ExamRepository examRepository,
            ExamAnswerRepository answerRepository, EvaluationReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.examRepository = examRepository;
        this.answerRepository = answerRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = "exam-attempt-results", key = "#attemptId + ':' + #requesterId")
    public ExamResultResponse execute(UUID attemptId, UUID requesterId, boolean administrator) {
        if (attemptId == null || requesterId == null) {
            throw new IllegalArgumentException("El intento y el usuario son obligatorios");
        }
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("El intento no existe"));
        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));
        boolean ownsAttempt = attempt.getStudentId().equals(requesterId);
        boolean ownsExam = exam.getCreatedByUserId().equals(requesterId);
        if (!administrator) {
            if (!ownsAttempt && !ownsExam) {
                throw new AccessDeniedException("No puedes consultar el resultado de este intento");
            }
            // El resultado incluye la llave de respuestas (isCorrect por opción) y el
            // puntaje calculado; un alumno no debe poder leerlo mientras el intento
            // sigue abierto (se haría trampa respondiendo). Profesor dueño del examen
            // y administrador sí pueden ver el avance en vivo.
            if (ownsAttempt && !ownsExam && !attempt.isSubmitted()) {
                throw new IllegalStateException("El intento aún no ha sido entregado");
            }
        }
        return calculate(attempt, exam);
    }

    public List<ExamResultResponse> executeForExam(UUID examId, UUID requesterId, boolean administrator) {
        if (examId == null || requesterId == null) {
            throw new IllegalArgumentException("El examen y el usuario son obligatorios");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!administrator && !exam.getCreatedByUserId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes consultar resultados de otro profesor");
        }
        return attemptRepository.findByExamId(examId).stream()
                .map(attempt -> calculate(attempt, exam))
                .toList();
    }

    public List<ExamResultResponse> executeLatestForExam(UUID examId, UUID requesterId, boolean administrator) {
        if (examId == null || requesterId == null) {
            throw new IllegalArgumentException("El examen y el usuario son obligatorios");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!administrator && !exam.getCreatedByUserId().equals(requesterId)) {
            throw new AccessDeniedException("No puedes consultar resultados de otro profesor");
        }
        return attemptRepository.findByExamId(examId).stream()
                .collect(Collectors.toMap(
                        ExamAttempt::getStudentId,
                        attempt -> attempt,
                        BinaryOperator.maxBy(Comparator.comparing(ExamAttempt::getStartedAt))))
                .values().stream()
                .map(attempt -> calculate(attempt, exam))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentExamHistoryResponse> executeStudentHistory(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("El estudiante es obligatorio");
        }
        return attemptRepository.findByStudentId(studentId).stream()
                .filter(ExamAttempt::isSubmitted)
                .map(attempt -> {
                    Exam exam = examRepository.findById(attempt.getExamId())
                            .orElseThrow(() -> new ExamNotFoundException(attempt.getExamId()));
                    ExamResultResponse result = calculate(attempt, exam);
                    return new StudentExamHistoryResponse(
                            attempt.getId(), exam.getId(), exam.getTitle(), attempt.getStartedAt(),
                            attempt.getSubmittedAt(), result.score(), result.maxScore(), result.pendingReview(),
                            attempt.isIntegrityFailed());
                })
                .sorted(Comparator.comparing(StudentExamHistoryResponse::submittedAt).reversed())
                .toList();
    }

    public PagedExamResultsResponse executeForExamPage(UUID examId, UUID requesterId, boolean administrator,
            int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw new IllegalArgumentException("La pagina debe ser >= 0 y el tamano debe estar entre 1 y 200");
        }
        List<ExamResultResponse> all = executeForExam(examId, requesterId, administrator);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int totalPages = all.isEmpty() ? 0 : (int) Math.ceil((double) all.size() / size);
        return new PagedExamResultsResponse(all.subList(from, to), page, size, all.size(), totalPages);
    }

    @Cacheable(cacheNames = "exam-averages", key = "#examId + ':' + #requesterId")
    public ExamAverageResponse calculateAverage(UUID examId, UUID requesterId, boolean administrator) {
        List<ExamResultResponse> results = executeForExam(examId, requesterId, administrator);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));

        int totalAttempts = results.size();
        int maxScore = exam.getTotalPoints();
        int pendingReviewCount = 0;
        double totalScore = 0;

        for (ExamResultResponse result : results) {
            totalScore += result.score();
            if (result.pendingReview()) {
                pendingReviewCount++;
            }
        }

        double averageScore = totalAttempts > 0 ? totalScore / totalAttempts : 0.0;
        double averagePercentage = maxScore > 0 && totalAttempts > 0
                ? (averageScore / maxScore) * 100.0
                : 0.0;

        return new ExamAverageResponse(
                examId,
                exam.getTitle(),
                totalAttempts,
                totalAttempts - pendingReviewCount,
                pendingReviewCount,
                averageScore,
                maxScore,
                averagePercentage);
    }

    private ExamResultResponse calculate(ExamAttempt attempt, Exam exam) {
        Map<UUID, ExamAnswer> answers = new HashMap<>();
        answerRepository.findByAttemptId(attempt.getId())
                .forEach(answer -> answers.put(answer.getQuestionId(), answer));

        int score = 0;
        boolean pendingReview = false;
        List<ExamResultResponse.QuestionResult> details = new java.util.ArrayList<>();
        for (Question question : exam.getQuestions()) {
            ExamAnswer answer = answers.get(question.getId());
            int questionScore = 0;
            boolean questionPending = false;
            String feedback = null;
            if (answer != null && question.getType() == QuestionType.OPEN_ENDED) {
                var review = reviewRepository.findByAnswerId(answer.getId());
                questionPending = review.isEmpty();
                // La revisión ya viene validada contra los puntos de la pregunta al
                // guardarla (ReviewOpenEndedAnswerUseCase); aquí solo se suma.
                questionScore = review.map(EvaluationReview::getScore).orElse(0);
                feedback = review.map(EvaluationReview::getFeedback).orElse(null);
            } else if (answer != null) {
                questionScore = isCorrectlyAnswered(question, answer) ? question.getPoints() : 0;
            }
            score += questionScore;
            pendingReview = pendingReview || questionPending;
            details.add(toQuestionResult(question, answer, questionScore, questionPending, feedback));
        }

        StudentContact contact = resolveStudent(attempt.getStudentId());
        return new ExamResultResponse(attempt.getId(), exam.getId(), attempt.getStudentId(),
                contact.email(), contact.firstName(), contact.lastName(), contact.phone(),
                attempt.isIntegrityFailed() ? 0 : score, exam.getTotalPoints(), pendingReview,
                attempt.isIntegrityFailed(), details);
    }

    /** Puntúa una opción múltiple: solo suma si el conjunto seleccionado es exactamente el correcto. */
    private boolean isCorrectlyAnswered(Question question, ExamAnswer answer) {
        Set<UUID> selected = new HashSet<>(answer.getSelectedOptionIds());
        Set<UUID> correct = question.getOptions().stream()
                .filter(edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption::isCorrect)
                .map(edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption::getId)
                .collect(java.util.stream.Collectors.toSet());
        return selected.equals(correct);
    }

    private ExamResultResponse.QuestionResult toQuestionResult(Question question, ExamAnswer answer,
            int questionScore, boolean questionPending, String feedback) {
        List<ExamResultResponse.QuestionResult.OptionDetail> optionDetails = question.getOptions().stream()
                .map(opt -> new ExamResultResponse.QuestionResult.OptionDetail(
                        opt.getId(), opt.getText(), opt.isCorrect()))
                .toList();
        List<UUID> selectedIds = answer != null ? answer.getSelectedOptionIds() : List.of();
        String openText = answer != null ? answer.getOpenTextAnswer() : null;
        UUID answerId = answer != null ? answer.getId() : null;
        return new ExamResultResponse.QuestionResult(question.getId(), answerId, question.getPrompt(),
                question.getType(), selectedIds, openText, optionDetails,
                answer != null, questionScore, question.getPoints(), questionPending, feedback);
    }

    /** Datos de contacto del alumno; el intento puede pertenecer a un usuario ya borrado. */
    private StudentContact resolveStudent(UUID studentId) {
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null) {
            return new StudentContact(null, null, null, null);
        }
        String phone = student.getPhones().stream()
                .filter(Phone::isPrimary)
                .findFirst()
                .map(Phone::getNumber)
                .orElse(student.getPhones().isEmpty() ? null : student.getPhones().get(0).getNumber());
        return new StudentContact(student.getEmail(), student.getFirstNames(), student.getLastNames(), phone);
    }

    private record StudentContact(String email, String firstName, String lastName, String phone) {
    }
}