package edu.cidemags.integrity_core.sentinel.ports.out;

import edu.cidemags.integrity_core.sentinel.domain.EventType;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;

import java.time.Duration;
import java.util.List;

public interface EventWindowPort {

    void record(SuspiciousEvent event);

    List<SuspiciousEvent> findRecentEvents(
        ExamSessionId examSessionId,
        StudentId studentId,
        EventType type,
        Duration window
    );
}