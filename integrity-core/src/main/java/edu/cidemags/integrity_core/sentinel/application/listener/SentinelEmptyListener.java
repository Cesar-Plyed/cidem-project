package edu.cidemags.integrity_core.sentinel.application.listener;

import edu.cidemags.integrity_core.events.domain.EventPublisher;
import org.springframework.stereotype.Component;

/**
 * Listener vacio del modulo SENTINEL: no procesa eventos externos hasta Sprint 2.
 */
@Component
public class SentinelEmptyListener implements EventPublisher {

    @Override
    public void publish(Object event) {
        // TODO Sprint 2: implementar logica de publicacion
    }

    @Override
    public void subscribe(Class<?> eventType) {
        // TODO Sprint 2: registrar tipo de evento
    }
}
