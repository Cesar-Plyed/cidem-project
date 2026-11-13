package edu.cidemags.integrity_core.safeguard.application.service;

import java.util.List;
import java.util.Optional;

import edu.cidemags.integrity_core.safeguard.domain.model.BlockEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;

/**
 * Validador de restricciones SafeGuard (spec 19). Servicio puro Java, sin
 * dependencias de framework ni de persistencia: quien lo llama aporta la lista
 * de restricciones configuradas y el número de ocurrencias ya observadas del
 * tipo dentro de la ventana.
 *
 * <p>Decide si un evento debe bloquearse:</p>
 * <ul>
 *   <li>Sin restricción para el tipo indicado → permitido (vacío).</li>
 *   <li>Restricción encontrada pero deshabilitada → ignorada (vacío).</li>
 *   <li>Ocurrencias dentro de la ventana &gt; {@code maxOccurrences} → {@link BlockEvent}.</li>
 *   <li>En cualquier otro caso → permitido (vacío).</li>
 * </ul>
 */
public class RestrictionValidator {

    /**
     * @param restrictions        restricciones configuradas para el examen (puede ser la
     *                            configuración por defecto cuando no hay filas persistidas)
     * @param type                tipo de restricción del evento a evaluar
     * @param occurrencesInWindow ocurrencias del mismo tipo observadas dentro de la ventana
     * @return {@link BlockEvent} si el evento supera el umbral; vacío si está permitido o se ignora
     */
    public Optional<BlockEvent> validate(List<Restriction> restrictions, RestrictionType type,
                                         long occurrencesInWindow) {
        if (restrictions == null || type == null) {
            return Optional.empty();
        }
        return restrictions.stream()
                .filter(restriction -> restriction.type() == type)
                .findFirst()
                // Deshabilitada => se ignora el tipo: nunca bloquea.
                .filter(Restriction::enabled)
                // El límite es inclusivo: maxOccurrences eventos son aceptables, el siguiente bloquea.
                .filter(restriction -> occurrencesInWindow > restriction.maxOccurrences())
                .map(restriction -> new BlockEvent(
                        type,
                        reason(restriction, occurrencesInWindow),
                        (int) occurrencesInWindow,
                        restriction.windowSeconds()));
    }

    private static String reason(Restriction restriction, long occurrences) {
        return String.format(
                "El evento %s superó el máximo de %d ocurrencias en %d s (%d detectadas)",
                restriction.type().name(), restriction.maxOccurrences(),
                restriction.windowSeconds(), occurrences);
    }
}
