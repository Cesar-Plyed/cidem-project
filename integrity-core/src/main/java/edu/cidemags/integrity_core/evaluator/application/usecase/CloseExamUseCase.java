package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CloseExamUseCase {
    private final ExamRepository examRepository;

    public CloseExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional
    public Exam execute(UUID examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ExamNotFoundException(examId));
        exam.close();
        return examRepository.save(exam);
    }
}