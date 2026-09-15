package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.evaluator.domain.model.Question;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.QuestionEntity;
import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.QuestionOptionEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class QuestionMapper {

    public Question toDomain(QuestionEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Question(
                entity.getId(),
                entity.getPrompt(),
                entity.getType(),
                entity.getOptions().stream().map(this::toOptionDomain).collect(Collectors.toList()),
                entity.getCorrectAnswerText()
        );
    }

    public QuestionEntity toEntity(Question domain) {
        if (domain == null) {
            return null;
        }

        QuestionEntity entity = new QuestionEntity(
                domain.getId(),
                null,
                domain.getPrompt(),
                domain.getType(),
                domain.getCorrectAnswerText()
        );

        entity.setOptions(domain.getOptions().stream().map(this::toOptionEntity).collect(Collectors.toList()));
        return entity;
    }

    private edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption toOptionDomain(QuestionOptionEntity entity) {
        return new edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption(
                entity.getId(),
                entity.getText(),
                entity.isCorrect());
    }

    private QuestionOptionEntity toOptionEntity(edu.cidemags.integrity_core.evaluator.domain.model.QuestionOption domain) {
        return new QuestionOptionEntity(
                domain.getId(),
                null,
                domain.getText(),
                domain.isCorrect());
    }
}
