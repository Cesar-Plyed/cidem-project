package edu.cidemags.integrity_core.sentinel.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamSessionIdTest {

    @Test
    void shouldCreateExamSessionIdWithValidUuid() {
        UUID value = UUID.randomUUID();

        ExamSessionId examSessionId = new ExamSessionId(value);

        assertThat(examSessionId.value()).isEqualTo(value);
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        assertThatThrownBy(() -> new ExamSessionId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("examSessionId");
    }
}