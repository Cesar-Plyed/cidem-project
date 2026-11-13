package edu.cidemags.integrity_core.legal.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AuditControllerTest {

    @Test
    void parsesIsoInstant() {
        assertThat(AuditController.parse("2026-01-01T10:00:00Z", true))
                .isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void parsesSimpleDateAtStartAndEndOfDay() {
        assertThat(AuditController.parse("2026-01-01", false))
                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(AuditController.parse("2026-01-01", true))
                .isEqualTo(Instant.parse("2026-01-01T23:59:59.999999999Z"));
    }

    @Test
    void nullDatesStayNull() {
        assertThat(AuditController.parse(null, true)).isNull();
        assertThat(AuditController.parse("", false)).isNull();
    }
}
