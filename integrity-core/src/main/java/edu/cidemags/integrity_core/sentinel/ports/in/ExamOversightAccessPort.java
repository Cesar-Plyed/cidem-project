package edu.cidemags.integrity_core.sentinel.ports.in;

import java.util.UUID;

public interface ExamOversightAccessPort {
    boolean canManageExam(UUID examId, UUID requesterId);
}
