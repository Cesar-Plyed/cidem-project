package edu.cidemags.integrity_core.sentinel.ports.out;

import java.util.List;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;

public interface AlertRepositoryPort {

    void save(Alert alert);

    List<Alert> findByExamSessionId(ExamSessionId examSessionId);

    
} 
