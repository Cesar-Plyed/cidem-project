package edu.cidemags.integrity_core.evaluator.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ExamAnswer {
    private final UUID id;
    private final UUID attemptId;
    private final UUID questionId;
    private final List<UUID> selectedOptionIds;
    private final String openTextAnswer;

    public ExamAnswer(UUID attemptId, UUID questionId) {
        this(UUID.randomUUID(), attemptId, questionId, new ArrayList<>(), null);
    }

    public ExamAnswer(UUID id, UUID attemptId, UUID questionId, List<UUID> selectedOptionIds, String openTextAnswer) {
        if (attemptId == null) {
            throw new IllegalArgumentException("La entrega es obligatoria");
        }
        if (questionId == null) {
            throw new IllegalArgumentException("La pregunta es obligatoria");
        }

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

    public UUID getQuestionId() {
        return questionId;
    }

    public List<UUID> getSelectedOptionIds() {
        return Collections.unmodifiableList(selectedOptionIds);
    }

    public String getOpenTextAnswer() {
        return openTextAnswer;
    }

    public void addSelectedOption(UUID optionId) {
        if (optionId == null) {
            throw new IllegalArgumentException("La opcion no puede ser nula");
        }
        this.selectedOptionIds.add(optionId);
    }
}
