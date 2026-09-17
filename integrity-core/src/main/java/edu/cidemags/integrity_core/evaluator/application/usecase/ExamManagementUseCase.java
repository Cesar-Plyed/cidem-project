package edu.cidemags.integrity_core.evaluator.application.usecase;

import edu.cidemags.integrity_core.evaluator.application.dto.UpdateExamCommand;
import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExamManagementUseCase {
    private final GetExamUseCase getExamUseCase;
    private final ListExamsUseCase listExamsUseCase;
    private final UpdateExamUseCase updateExamUseCase;
    private final DeleteExamUseCase deleteExamUseCase;
    private final PublishExamUseCase publishExamUseCase;
    private final CloseExamUseCase closeExamUseCase;

    public ExamManagementUseCase(GetExamUseCase getExamUseCase,
            ListExamsUseCase listExamsUseCase,
            UpdateExamUseCase updateExamUseCase,
            DeleteExamUseCase deleteExamUseCase,
            PublishExamUseCase publishExamUseCase,
            CloseExamUseCase closeExamUseCase) {
        this.getExamUseCase = getExamUseCase;
        this.listExamsUseCase = listExamsUseCase;
        this.updateExamUseCase = updateExamUseCase;
        this.deleteExamUseCase = deleteExamUseCase;
        this.publishExamUseCase = publishExamUseCase;
        this.closeExamUseCase = closeExamUseCase;
    }

    public List<Exam> list() {
        return listExamsUseCase.execute();
    }

    public Exam get(UUID examId) {
        return getExamUseCase.execute(examId);
    }

    public Exam update(UpdateExamCommand command) {
        return updateExamUseCase.execute(command);
    }

    public void delete(UUID examId) {
        deleteExamUseCase.execute(examId);
    }

    public Exam publish(UUID examId) {
        return publishExamUseCase.execute(examId);
    }

    public Exam close(UUID examId) {
        return closeExamUseCase.execute(examId);
    }
}