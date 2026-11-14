package edu.cidemags.integrity_core.legal.application.service;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.DataBreachNotification;
import edu.cidemags.integrity_core.legal.domain.model.BreachStatus;
import edu.cidemags.integrity_core.legal.domain.model.BreachType;
import edu.cidemags.integrity_core.legal.infrastructure.notification.BreachNotifier;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataBreachNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notificacion de brechas de datos (Art. 34 GDPR, spec 49). */
@Service
public class BreachNotificationService {

    private static final String DEFAULT_ACTION =
            "Cambia tu contrasena, revisa los accesos recientes y contacta con el DPO si detectas actividad sospechosa.";

    private final DataBreachNotificationRepository repository;
    private final BreachNotifier notifier;

    public BreachNotificationService(DataBreachNotificationRepository repository,
                                     BreachNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    @Transactional
    public DataBreachNotification notifyStudent(UUID studentId, BreachType breachType, String description) {
        DataBreachNotification notification = new DataBreachNotification(
                studentId, breachType, description, DEFAULT_ACTION);
        notification = repository.save(notification);

        String subject = "Aviso de seguridad: " + breachType.name();
        String body = description + " Accion recomendada: " + DEFAULT_ACTION;
        notifier.sendEmail(studentId, subject, body);
        notifier.sendPush(studentId, body);

        notification.markNotified();
        return repository.save(notification);
    }

    @Transactional
    public DataBreachNotification resolveNotification(UUID notificationId) {
        DataBreachNotification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notificacion de brecha no encontrada: " + notificationId));
        notification.resolve();
        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<LegalViews.BreachNotice> getPendingNotifications(UUID studentId) {
        return repository.findByStudentIdAndStatusNotOrderByNotifiedAtDesc(studentId, BreachStatus.RESOLVED)
                .stream()
                .map(n -> new LegalViews.BreachNotice(n.getId(), n.getBreachType().name(),
                        n.getDescription(), n.getRecommendedAction(), n.getNotifiedAt(),
                        n.getStatus().name()))
                .toList();
    }
}
