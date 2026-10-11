package edu.cidemags.integrity_core.evaluator.application.usecase;

import java.util.UUID;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.evaluator.application.dto.JoinExamByAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamAlreadyAttemptedException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamAccessCodeNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamClosedException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAttempt;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAttemptRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamParticipationRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;

@Service
public class JoinExamByAccessCodeUseCase {

    private final ExamRepository examRepository;
    private final ExamAccessCodeRepository examAccessCodeRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamParticipationRepository examParticipationRepository;

    public JoinExamByAccessCodeUseCase(ExamRepository examRepository,
            ExamAccessCodeRepository examAccessCodeRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamParticipationRepository examParticipationRepository) {
        this.examRepository = examRepository;
        this.examAccessCodeRepository = examAccessCodeRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examParticipationRepository = examParticipationRepository;
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

        List<ExamAttempt> attempts = findAttemptsForExam(exam.getId(), command.studentId());
        if (!attempts.isEmpty()) {
            return existingAttemptOrReject(exam.getId(), attempts);
        }

        // La reserva única por examen/alumno evita que códigos regenerados o dos
        // solicitudes simultáneas creen intentos distintos para la misma persona.
        if (!examParticipationRepository.reserve(exam.getId(), command.studentId())) {
            return existingAttemptOrReject(
                    exam.getId(), findAttemptsForExam(exam.getId(), command.studentId()));
        }
        return examAttemptRepository.save(new ExamAttempt(exam.getId(), command.studentId()));
    }

    @Transactional
    public ExamAttempt executeByCode(String code, UUID studentId) {
        if (code == null || code.isBlank() || studentId == null) {
            throw new IllegalArgumentException("Codigo y estudiante son obligatorios");
        }

        ExamAccessCode accessCode = examAccessCodeRepository.findByCode(code)
                .orElseThrow(() -> new ExamAccessCodeNotFoundException(null, code));
        return execute(new JoinExamByAccessCodeCommand(accessCode.getExamId(), code, studentId));
    }

    private List<ExamAttempt> findAttemptsForExam(UUID examId, UUID studentId) {
        return examAttemptRepository.findByStudentId(studentId).stream()
                .filter(attempt -> attempt.getExamId().equals(examId))
                .toList();
    }

    private ExamAttempt existingAttemptOrReject(UUID examId, List<ExamAttempt> attempts) {
        if (attempts.stream().anyMatch(ExamAttempt::isSubmitted)) {
            throw new ExamAlreadyAttemptedException(examId);
        }
        return attempts.stream()
                .max(Comparator.comparing(ExamAttempt::getStartedAt))
                .orElseThrow(() -> new ExamAlreadyAttemptedException(examId));
    }
}
