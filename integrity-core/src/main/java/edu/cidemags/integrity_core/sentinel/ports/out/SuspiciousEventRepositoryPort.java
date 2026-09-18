package edu.cidemags.integrity_core.sentinel.ports.out;

import java.util.List;

import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;

public interface SuspiciousEventRepositoryPort {

    void save(SuspiciousEvent event);

    List<SuspiciousEvent> findByExamSessionId(ExamSessionId examSessionId);

    
} 
