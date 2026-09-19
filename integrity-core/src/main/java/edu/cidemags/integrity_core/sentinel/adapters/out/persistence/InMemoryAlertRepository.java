package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryAlertRepository implements AlertRepositoryPort {

    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    @Override
    public void save(Alert alert) {
        alerts.add(alert);
    }

    @Override
    public List<Alert> findByExamSessionId(ExamSessionId examSessionId) {
        return new ArrayList<>(alerts.stream()
            .filter(alert -> alert.examSessionId().equals(examSessionId))
            .toList());
    }
}