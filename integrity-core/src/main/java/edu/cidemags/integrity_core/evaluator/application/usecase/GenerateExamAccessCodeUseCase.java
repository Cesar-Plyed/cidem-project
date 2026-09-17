package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.GenerateExamAccessCodeCommand;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.domain.model.ExamAccessCode;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamAccessCodeRepository;
import edu.cidemags.integrity_core.evaluator.domain.port.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
public class GenerateExamAccessCodeUseCase {

    private final ExamRepository examRepository;
    private final ExamAccessCodeRepository examAccessCodeRepository;

    public GenerateExamAccessCodeUseCase(ExamRepository examRepository,
            ExamAccessCodeRepository examAccessCodeRepository) {
        this.examRepository = examRepository;
        this.examAccessCodeRepository = examAccessCodeRepository;
    }

    @Transactional
    public ExamAccessCode execute(GenerateExamAccessCodeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        if (command.examId() == null) {
            throw new IllegalArgumentException("El identificador del examen es obligatorio");
        }
        if (command.expiresInMinutes() <= 0) {
            throw new IllegalArgumentException("La expiracion debe ser mayor que cero");
        }

        Exam exam = examRepository.findById(command.examId())
                .orElseThrow(() -> new ExamNotFoundException(command.examId()));
        if (exam.getStatus() != edu.cidemags.integrity_core.evaluator.domain.model.ExamStatus.PUBLISHED) {
            throw new IllegalStateException("Solo se puede generar un codigo para un examen publicado");
        }

        String accessCode = command.code() == null || command.code().isBlank()
                ? UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : command.code().trim().toUpperCase(Locale.ROOT);
        if (accessCode.length() < 4 || accessCode.length() > 50) {
            throw new IllegalArgumentException("El codigo debe tener entre 4 y 50 caracteres");
        }

        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneId.systemDefault()).plusMinutes(command.expiresInMinutes());
        ExamAccessCode generated = new ExamAccessCode(exam.getId(), accessCode, expiresAt);
        return examAccessCodeRepository.save(generated);
    }
}
