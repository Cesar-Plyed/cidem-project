package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.evaluator.domain.model.Exam;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.ExamEntity;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

    private final QuestionMapper questionMapper;

    public ExamMapper(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public Exam toDomain(ExamEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Exam(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                entity.getDurationMinutes(),
                entity.getQuestions().stream().map(questionMapper::toDomain).toList(),
                entity.getStatus());
    }

    public ExamEntity toEntity(Exam domain) {
        if (domain == null) {
            return null;
        }

        ExamEntity entity = new ExamEntity(
                domain.getId(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getCreatedByUserId(),
                domain.getCreatedAt(),
                domain.getDurationMinutes(),
                domain.getStatus());

        entity.setQuestions(domain.getQuestions().stream().map(questionMapper::toEntity).toList());
        entity.getQuestions().forEach(question -> question.setExam(entity));
        return entity;
    }
}
