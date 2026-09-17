package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListExamsUseCase {
    private final ExamRepository examRepository;

    public ListExamsUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public List<Exam> execute() {
        return examRepository.findAll();
    }
}