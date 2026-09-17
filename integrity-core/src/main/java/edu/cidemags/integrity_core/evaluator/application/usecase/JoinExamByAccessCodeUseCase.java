package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamAccessCodeNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamClosedException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JoinExamByAccessCodeUseCase {

    private final ExamRepository examRepository;
    private final ExamAccessCodeRepository examAccessCodeRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public JoinExamByAccessCodeUseCase(ExamRepository examRepository,
            ExamAccessCodeRepository examAccessCodeRepository,
            ExamAttemptRepository examAttemptRepository) {
        this.examRepository = examRepository;
        this.examAccessCodeRepository = examAccessCodeRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional
    public ExamAttempt execute(JoinExamByAccessCodeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        if (command.examId() == null || command.studentId() == null || command.code() == null
                || command.code().isBlank()) {
            throw new IllegalArgumentException("Examen, estudiante y codigo son obligatorios");
        }

        Exam exam = examRepository.findById(command.examId())
                .orElseThrow(() -> new ExamNotFoundException(command.examId()));

        if (exam.getStatus() == ExamStatus.CLOSED) {
            throw new ExamClosedException(command.examId());
        }
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("El examen aun no esta publicado");
        }

        ExamAccessCode accessCode = examAccessCodeRepository.findByExamIdAndCode(command.examId(), command.code())
                .orElseThrow(() -> new ExamAccessCodeNotFoundException(command.examId(), command.code()));

        if (!accessCode.isValid()) {
            throw new ExamAccessCodeNotFoundException(command.examId(), command.code());
        }

        ExamAttempt attempt = new ExamAttempt(exam.getId(), command.studentId());
        return examAttemptRepository.save(attempt);
    }
}
