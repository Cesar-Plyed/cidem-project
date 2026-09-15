package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigRegistrationCodeValidatorTest {

    private ConfigRegistrationCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigRegistrationCodeValidator("codigo-admin-secreto", "codigo-profesor-secreto");
    }

    @Test
    void this_student_should_never_require_code() {
        assertThat(validator.requiresCode(Role.ALUMNO)).isFalse();
    }

    @Test
    void student_should_be_valid_even_with_null_or_empty_code() {
        assertThat(validator.isValid(Role.ALUMNO, null)).isTrue();
        assertThat(validator.isValid(Role.ALUMNO, "cualquier-cosa")).isTrue();
    }

    @Test
    void teacher_and_administrative_if_should_require_code() {
        assertThat(validator.requiresCode(Role.PROFESOR)).isTrue();
        assertThat(validator.requiresCode(Role.ADMINISTRATIVO)).isTrue();
    }

    @Test
    void this_should_accept_the_correct_teacher_code() {
        assertThat(validator.isValid(Role.PROFESOR, "codigo-profesor-secreto")).isTrue();
    }

    @Test
    void this_should_reject_the_incorrect_teacher_code() {
        assertThat(validator.isValid(Role.PROFESOR, "codigo-equivocado")).isFalse();
    }

    @Test
    void this_should_reject_null_code_for_teacher() {
        assertThat(validator.isValid(Role.PROFESOR, null)).isFalse();
    }

    @Test
    void this_should_accept_the_correct_administrative_code() {
        assertThat(validator.isValid(Role.ADMINISTRATIVO, "codigo-admin-secreto")).isTrue();
    }

    @Test
    void the_teacher_code_NOT_should_serve_for_administrative() {
        assertThat(validator.isValid(Role.ADMINISTRATIVO, "codigo-profesor-secreto")).isFalse();
    }
}