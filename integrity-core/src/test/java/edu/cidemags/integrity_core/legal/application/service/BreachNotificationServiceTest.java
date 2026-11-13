package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.DataBreachNotification;
import edu.cidemags.integrity_core.legal.domain.model.BreachStatus;
import edu.cidemags.integrity_core.legal.domain.model.BreachType;
import edu.cidemags.integrity_core.legal.infrastructure.notification.BreachNotifier;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataBreachNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BreachNotificationServiceTest {

    @Mock
    private DataBreachNotificationRepository repository;

    @Mock
    private BreachNotifier notifier;

    @InjectMocks
    private BreachNotificationService service;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void notifyStudentSendsEmailAndPush() {
        when(repository.save(any(DataBreachNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        DataBreachNotification notification = service.notifyStudent(studentId,
                BreachType.DATA_LEAK, "Se detecto una fuga de datos");

        verify(notifier).sendEmail(eq(studentId), anyString(), anyString());
        verify(notifier).sendPush(eq(studentId), anyString());
        assertThat(notification.getStatus()).isEqualTo(BreachStatus.NOTIFIED);
        assertThat(notification.getRecommendedAction()).isNotBlank();
    }

    @Test
    void resolveNotificationMarksStatusAndDate() {
        DataBreachNotification stored = new DataBreachNotification(studentId,
                BreachType.UNAUTHORIZED_ACCESS, "Acceso indebido", "Cambia la contrasena");
        when(repository.findById(any())).thenReturn(Optional.of(stored));
        when(repository.save(any(DataBreachNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        DataBreachNotification resolved = service.resolveNotification(UUID.randomUUID());

        assertThat(resolved.getStatus()).isEqualTo(BreachStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    void pendingNotificationsExcludeResolved() {
        DataBreachNotification pending = new DataBreachNotification(studentId,
                BreachType.SYSTEM_COMPROMISE, "Compromiso", "Accion");
        when(repository.findByStudentIdAndStatusNotOrderByNotifiedAtDesc(studentId, BreachStatus.RESOLVED))
                .thenReturn(List.of(pending));

        var notifications = service.getPendingNotifications(studentId);

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).breachType()).isEqualTo("SYSTEM_COMPROMISE");
    }
}
