package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.ExamResultResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExportExamResultsUseCase {
    private final GetExamResultUseCase getExamResultUseCase;

    public ExportExamResultsUseCase(GetExamResultUseCase getExamResultUseCase) {
        this.getExamResultUseCase = getExamResultUseCase;
    }

    public String execute(UUID examId, UUID requesterId, boolean administrator) {
        List<ExamResultResponse> results = getExamResultUseCase.executeForExam(examId, requesterId, administrator);
        StringBuilder csv = new StringBuilder("attempt_id,student_id,score,max_score,pending_review\n");
        results.forEach(result -> csv.append(result.attemptId()).append(',')
                .append(result.studentId()).append(',')
                .append(result.score()).append(',')
                .append(result.maxScore()).append(',')
                .append(result.pendingReview()).append('\n'));
        return csv.toString();
    }
}