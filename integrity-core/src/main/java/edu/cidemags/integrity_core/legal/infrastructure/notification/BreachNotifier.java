package edu.cidemags.integrity_core.legal.infrastructure.notification;

import java.util.UUID;

/** Canal de notificacion de brechas (email + push). Spec 49. */
public interface BreachNotifier {

    void sendEmail(UUID studentId, String subject, String body);

    void sendPush(UUID studentId, String body);
}
