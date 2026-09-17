package edu.cidemags.integrity_core.evaluator.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exam_answers")
public class ExamAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @ElementCollection
    @CollectionTable(name = "exam_answer_selected_options", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id")
    private List<UUID> selectedOptionIds = new ArrayList<>();

    @Column(name = "open_text_answer", length = 4000)
    private String openTextAnswer;

    protected ExamAnswerEntity() {
    }

    public ExamAnswerEntity(UUID id, UUID attemptId, UUID questionId, List<UUID> selectedOptionIds,
            String openTextAnswer) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOptionIds = selectedOptionIds == null ? new ArrayList<>() : new ArrayList<>(selectedOptionIds);
        this.openTextAnswer = openTextAnswer;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(UUID attemptId) {
        this.attemptId = attemptId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public List<UUID> getSelectedOptionIds() {
        return selectedOptionIds;
    }

    public void setSelectedOptionIds(List<UUID> selectedOptionIds) {
        this.selectedOptionIds = selectedOptionIds;
    }

    public String getOpenTextAnswer() {
        return openTextAnswer;
    }

    public void setOpenTextAnswer(String openTextAnswer) {
        this.openTextAnswer = openTextAnswer;
    }
}
