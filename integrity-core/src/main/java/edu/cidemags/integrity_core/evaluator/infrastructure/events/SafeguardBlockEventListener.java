package edu.cidemags.integrity_core.evaluator.infrastructure.events;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

/**
 * Listener vacío de SafeGuardBlockEvent (ToDo.md, Sprint 1: "listeners vacíos").
 * La lógica de actualización de estado del intento llega en el Sprint 7; de momento
 * solo conserva el último evento recibido para que las pruebas de
 * publicación/recepción puedan verificar el cableado.
 */
@Component
public class SafeguardBlockEventListener {

    // AtomicReference en vez de volatile: los listeners pueden ejecutarse en
    // varios hilos y la referencia debe publicarse de forma atómica.
    private final AtomicReference<SafeguardBlockEvent> lastReceived = new AtomicReference<>();

    @EventListener
    public void onSafeguardBlock(SafeguardBlockEvent event) {
        // TODO Sprint 7 (ToDo.md): EVALUATOR actualiza el estado del intento a partir
        // de este evento. Ahora solo se captura para pruebas.
        this.lastReceived.set(event);
    }

    public SafeguardBlockEvent getLastReceived() {
        return lastReceived.get();
    }
}
