package edu.cidemags.integrity_core.events.domain;

import java.util.EventListener;

/**
 * Contrato de publicacion/recepcion de eventos compartidos entre modulos.
 * Cada modulo registra una implementación vacia hasta el Sprint 2.
 * <p>
 * El metodo {@code publish} se usa para enviar un evento a los listeners
 * registrados (Spring {@code ApplicationEventPublisher} o un broker futuro).
 * </p>
 */
public interface EventPublisher extends EventListener {

    /**
     * Publica un evento a todos los listeners registrados.
     * @param event objeto evento a publicar
     */
    void publish(Object event);

    /**
     * Registra un listener que recibe eventos de este tipo.
     */
    void subscribe(Class<?> eventType);
}
