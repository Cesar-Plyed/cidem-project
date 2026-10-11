package edu.cidemags.integrity_core.evaluator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rejectsQuestionsWhosePointsExceedTenInTotal() {
        Exam exam = new Exam("Historia", null, UUID.randomUUID());
        exam.addQuestion(openQuestion("Primera", 5));
        exam.addQuestion(openQuestion("Segunda", 5));

        // El examen ya está lleno a su tope de 10: otra pregunta lo desborda.
        assertThrows(IllegalArgumentException.class, () -> exam.addQuestion(openQuestion("Tercera", 1)));
        assertEquals(10, exam.getTotalPoints());
    }

    @Test
    void replacesQuestionsOnlyWhenDraftAndWithinTheTenPointCap() {
        Exam exam = new Exam("Historia", null, UUID.randomUUID());
        exam.addQuestion(openQuestion("Vieja", 3));

        exam.replaceQuestions(List.of(openQuestion("Nueva", 7)));
        assertEquals(1, exam.getQuestions().size());
        assertEquals(7, exam.getTotalPoints());

        assertThrows(IllegalArgumentException.class,
                () -> exam.replaceQuestions(List.of(openQuestion("A", 6), openQuestion("B", 6))));

        exam.publish();
        assertThrows(IllegalStateException.class,
                () -> exam.replaceQuestions(List.of(openQuestion("C", 1))));
    }

    private static Question openQuestion(String prompt, int points) {
        return new Question(UUID.randomUUID(), prompt, QuestionType.OPEN_ENDED, List.of(), null, points);
    }
}
