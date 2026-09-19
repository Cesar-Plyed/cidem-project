package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemorySuspiciousEventRepository implements SuspiciousEventRepositoryPort {

    private final List<SuspiciousEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void save(SuspiciousEvent event) {
        events.add(event);
    }

    @Override
    public List<SuspiciousEvent> findByExamSessionId(ExamSessionId examSessionId) {
        return new ArrayList<>(events.stream()
            .filter(event -> event.examSessionId().equals(examSessionId))
            .toList());
    }
}