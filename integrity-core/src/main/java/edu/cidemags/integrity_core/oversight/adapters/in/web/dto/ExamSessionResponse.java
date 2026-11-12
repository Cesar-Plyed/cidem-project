package edu.cidemags.integrity_core.oversight.adapters.in.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for GET /oversight/exams/{examId}/sessions.
 */
public record ExamSessionResponse(
    UUID sessionId,
    String examName,
    String status,
    int studentCount,
    Instant lastActivity
) {

    public static ExamSessionResponse from(
        UUID sessionId,
        String examName,
        String status,
        int studentCount,
        Instant lastActivity
    ) {
        return new ExamSessionResponse(sessionId, examName, status, studentCount, lastActivity);
    }
}
