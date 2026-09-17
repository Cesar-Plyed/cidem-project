package edu.cidemags.integrity_core.sentinel.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentIdTest {

    @Test
    void shouldCreateStudentIdWithValidUuid() {
        UUID value = UUID.randomUUID();

        StudentId studentId = new StudentId(value);

        assertThat(studentId.value()).isEqualTo(value);
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        assertThatThrownBy(() -> new StudentId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("studentId");
    }
}