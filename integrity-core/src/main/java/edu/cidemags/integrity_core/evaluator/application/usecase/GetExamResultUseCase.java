package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ExamAverageResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import edu.cidemags.integrity_core.evaluator.application.dto.PagedExamResultsResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
        if (!administrator) {
            boolean isStudent = attempt.getStudentId().equals(requesterId);
            boolean isTeacher = exam.getCreatedByUserId().equals(requesterId);
            if (!isStudent && !isTeacher) {
                throw new AccessDeniedException("No puedes consultar el resultado de este intento");
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
        int maxScore = exam.getQuestions().size();
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
                questionScore = review.map(value -> value.getScore()).orElse(0);
                feedback = review.map(EvaluationReview::getFeedback).orElse(null);
            } else if (answer != null) {
                Set<UUID> selected = new HashSet<>(answer.getSelectedOptionIds());
                Set<UUID> correct = question.getOptions().stream()
                        .filter(edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption::isCorrect)
                        .map(edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption::getId)
                        .collect(java.util.stream.Collectors.toSet());
                questionScore = selected.equals(correct) ? 1 : 0;
            }
            score += questionScore;
            pendingReview = pendingReview || questionPending;
            List<ExamResultResponse.QuestionResult.OptionDetail> optionDetails = question.getOptions().stream()
                    .map(opt -> new ExamResultResponse.QuestionResult.OptionDetail(
                            opt.getId(), opt.getText(), opt.isCorrect()))
                    .toList();
            List<UUID> selectedIds = answer != null ? answer.getSelectedOptionIds() : List.of();
            String openText = answer != null ? answer.getOpenTextAnswer() : null;
            details.add(new ExamResultResponse.QuestionResult(question.getId(), question.getPrompt(),
                    question.getType(), selectedIds, openText, optionDetails,
                    answer != null, questionScore, questionPending, feedback));
        }

        User student = userRepository.findById(attempt.getStudentId()).orElse(null);
        String studentEmail = student != null ? student.getEmail() : null;
        String studentFirstName = student != null ? student.getFirstNames() : null;
        String studentLastName = student != null ? student.getLastNames() : null;
        String studentPhone = student != null ? student.getPhones().stream()
                .filter(Phone::isPrimary)
                .findFirst()
                .map(Phone::getNumber)
                .orElse(student.getPhones().isEmpty() ? null : student.getPhones().get(0).getNumber())
                : null;

        return new ExamResultResponse(attempt.getId(), exam.getId(), attempt.getStudentId(),
                studentEmail, studentFirstName, studentLastName, studentPhone,
                score, exam.getQuestions().size(), pendingReview, details);
    }
}