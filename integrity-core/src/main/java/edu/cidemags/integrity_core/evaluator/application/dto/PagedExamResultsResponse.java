package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;

public record PagedExamResultsResponse(List<ExamResultResponse> content, int page, int size, long totalElements,
        int totalPages) {
}