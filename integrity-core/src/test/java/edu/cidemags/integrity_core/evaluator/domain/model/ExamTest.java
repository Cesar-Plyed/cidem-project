package edu.cidemags.integrity_core.evaluator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamTest {
    @Test
    void cannotPublishExamWithoutQuestions() {
        Exam exam = new Exam("Historia", null, UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, exam::publish);
    }

    @Test
    void cannotCloseDraftExam() {
        Exam exam = new Exam("Historia", null, UUID.randomUUID());

        assertThrows(IllegalStateException.class, exam::close);
    }
}