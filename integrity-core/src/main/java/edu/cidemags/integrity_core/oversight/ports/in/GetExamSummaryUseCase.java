package edu.cidemags.integrity_core.oversight.ports.in;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;

import java.util.List;
import java.util.UUID;

public interface GetExamSummaryUseCase {
    List<IntegritySnapshot> getSummary(UUID examSessionId);
}