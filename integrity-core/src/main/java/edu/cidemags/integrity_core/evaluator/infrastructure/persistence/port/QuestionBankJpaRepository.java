package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.port;

import edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity.QuestionBankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionBankJpaRepository extends JpaRepository<QuestionBankEntity, UUID> {
}
