package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_banks")
public class QuestionBankEntity {

    @Id
    private UUID id;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id")
    private List<QuestionEntity> questions = new ArrayList<>();

    protected QuestionBankEntity() {
    }

    public QuestionBankEntity(UUID id, String name, String description) {
        this(id, name, description, null);
    }

    public QuestionBankEntity(UUID id, String name, String description, UUID createdByUserId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByUserId = createdByUserId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<QuestionEntity> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionEntity> questions) {
        this.questions = questions;
    }
}
