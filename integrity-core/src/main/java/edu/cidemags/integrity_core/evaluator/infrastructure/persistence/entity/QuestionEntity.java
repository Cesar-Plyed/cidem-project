package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class QuestionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private ExamEntity exam;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Column(name = "correct_answer_text", length = 1000)
    private String correctAnswerText;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionOptionEntity> options = new ArrayList<>();

    protected QuestionEntity() {
    }

    public QuestionEntity(UUID id, ExamEntity exam, String prompt, QuestionType type, String correctAnswerText) {
        this.id = id;
        this.exam = exam;
        this.prompt = prompt;
        this.type = type;
        this.correctAnswerText = correctAnswerText;
    }

    public UUID getId() {
        return id;
    }

    public ExamEntity getExam() {
        return exam;
    }

    public void setExam(ExamEntity exam) {
        this.exam = exam;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getCorrectAnswerText() {
        return correctAnswerText;
    }

    public void setCorrectAnswerText(String correctAnswerText) {
        this.correctAnswerText = correctAnswerText;
    }

    public List<QuestionOptionEntity> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionEntity> options) {
        this.options = options;
    }
}
