package edu.cidemags.integrity_core.legal.infrastructure.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementacion por defecto: deja la notificacion en el log estructurado.
 *
 * <p>Se sustituye por un adaptador SMTP/FCM sin tocar el servicio legal.</p>
 */
@Component
public class LoggingBreachNotifier implements BreachNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingBreachNotifier.class);

    @Override
    public void sendEmail(UUID studentId, String subject, String body) {
        log.warn("Email de brecha para {}: {} - {}", studentId, subject, body);
    }

    @Override
    public void sendPush(UUID studentId, String body) {
        log.warn("Push de brecha para {}: {}", studentId, body);
    }
}
