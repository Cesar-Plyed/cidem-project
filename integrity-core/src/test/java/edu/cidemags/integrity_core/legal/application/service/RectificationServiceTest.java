package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RectificationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private RectificationService service;

    private final UUID id = UUID.randomUUID();

    @Test
    void rectifyEmailUpdatesUsersTable() {
        service.rectifyData("USER", id, "email", "new@email.com");

        verify(jdbc).update("update users set email = ? where id = ?", "new@email.com", id);
    }

    @Test
    void rectifyPhoneUpdatesPhoneNumbersTable() {
        service.rectifyData("PHONE", id, "phoneNumber", "+34123456789");

        verify(jdbc).update("update phone_numbers set phone_number = ? where id = ?", "+34123456789", id);
    }

    @Test
    void softDeleteMarksRowAsDeleted() {
        service.softDeleteData("USER", id);

        verify(jdbc).update("update users set is_deleted = true where id = ?", id);
    }

    @Test
    void rejectsUnknownEntityOrField() {
        assertThatThrownBy(() -> service.rectifyData("USER", id, "passwordHash", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rectifyData("INVOICE", id, "email", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
