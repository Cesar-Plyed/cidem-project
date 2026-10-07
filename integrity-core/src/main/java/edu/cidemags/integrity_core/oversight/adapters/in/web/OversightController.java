package edu.cidemags.integrity_core.oversight.adapters.in.web;

import edu.cidemags.integrity_core.oversight.ports.in.GetAttemptIntegrityUseCase;
import edu.cidemags.integrity_core.oversight.ports.in.GetExamSummaryUseCase;
import edu.cidemags.integrity_core.oversight.ports.in.GetExamTimelineUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/oversight")
@PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
public class OversightController {

    private final GetExamSummaryUseCase getExamSummaryUseCase;
    private final GetExamTimelineUseCase getExamTimelineUseCase;
    private final GetAttemptIntegrityUseCase getAttemptIntegrityUseCase;

    public OversightController(
        GetExamSummaryUseCase getExamSummaryUseCase,
        GetExamTimelineUseCase getExamTimelineUseCase,
        GetAttemptIntegrityUseCase getAttemptIntegrityUseCase
    ) {
        this.getExamSummaryUseCase = getExamSummaryUseCase;
        this.getExamTimelineUseCase = getExamTimelineUseCase;
        this.getAttemptIntegrityUseCase = getAttemptIntegrityUseCase;
    }

    @GetMapping("/exams/{examId}/summary")
    public ResponseEntity<List<IntegritySnapshotResponse>> getSummary(@PathVariable UUID examId) {
        List<IntegritySnapshotResponse> response = getExamSummaryUseCase.getSummary(examId).stream()
            .map(IntegritySnapshotResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams/{examId}/timeline")
    public ResponseEntity<List<LogEntryResponse>> getTimeline(@PathVariable UUID examId) {
        List<LogEntryResponse> response = getExamTimelineUseCase.getTimeline(examId).stream()
            .map(LogEntryResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attempts/{attemptId}/integrity")
    public ResponseEntity<IntegritySnapshotResponse> getIntegrity(@PathVariable UUID attemptId) {
        return getAttemptIntegrityUseCase.getIntegrity(attemptId)
            .map(IntegritySnapshotResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
} 