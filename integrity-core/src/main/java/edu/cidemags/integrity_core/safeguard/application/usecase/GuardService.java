package edu.cidemags.integrity_core.safeguard.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.safeguard.application.dto.GuardEventCommand;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventCommand;
import edu.cidemags.integrity_core.safeguard.application.service.RestrictionValidator;
import edu.cidemags.integrity_core.safeguard.domain.model.BlockEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.port.RestrictionRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardEventRepository;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

/**
 * Servicio de guardia (spec 20): registra un evento de restricción detectado
 * reutilizando la persistencia existente ({@code SafeguardEvent} vía
 * {@link SafeguardService#recordEvent}) y, cuando el umbral configurado se
 * supera, publica el contrato compartido {@link SafeguardBlockEvent} para que lo
 * consuman EVALUATOR y OVERSIGHT.
 *
 * <p>No existe una tabla {@code guard_events} paralela: el registro reutiliza la
 * sesión y el evento de SafeGuard ya modelados.</p>
 */
@Service
public class GuardService {

    private final SafeguardService safeguardService;
    private final SafeguardEventRepository eventRepository;
    private final RestrictionRepository restrictionRepository;
    private final ApplicationEventPublisher publisher;
    private final RestrictionValidator validator = new RestrictionValidator();

    public GuardService(SafeguardService safeguardService,
                        SafeguardEventRepository eventRepository,
                        RestrictionRepository restrictionRepository,
                        ApplicationEventPublisher publisher) {
        this.safeguardService = safeguardService;
        this.eventRepository = eventRepository;
        this.restrictionRepository = restrictionRepository;
        this.publisher = publisher;
    }

    /**
     * Registra el evento detectado y, si supera la restricción configurada para su
     * tipo, publica un {@link SafeguardBlockEvent}.
     *
     * @return el {@link BlockEvent} si el tipo fue bloqueado; vacío si está permitido
     */
    @Transactional
    public Optional<BlockEvent> registerRestrictionEvent(GuardEventCommand cmd) {
        SafeguardEventType eventType = toEventType(cmd.type());

        // 1) Registro con la persistencia existente (idempotente por session+eventId).
        safeguardService.recordEvent(new RecordEventCommand(
                cmd.sessionId(), cmd.studentId(), cmd.eventId(), eventType, cmd.occurredAt(), cmd.metadata()));

        // 2) Ocurrencias acumuladas del tipo en la sesión (incluye la recién registrada).
        long occurrences = eventRepository.countBySessionIdAndEventType(cmd.sessionId(), eventType);

        // 3) Restricciones del examen; sin filas persistidas se usan las por defecto.
        List<Restriction> restrictions = effectiveRestrictions(cmd.examId());

        // 4) Decisión pura de dominio y publicación del bloqueo.
        Optional<BlockEvent> block = validator.validate(restrictions, cmd.type(), occurrences);
        block.ifPresent(b -> publisher.publishEvent(new SafeguardBlockEvent(
                cmd.eventId(),
                cmd.examId(),
                cmd.attemptId(),
                cmd.sessionId(),
                cmd.studentId(),
                cmd.occurredAt() == null ? Instant.now() : cmd.occurredAt(),
                SafeguardBlockEvent.SOURCE_SAFEGUARD,
                Map.of(
                        "type", b.type().name(),
                        "maxOccurrences", maxOccurrencesFor(restrictions, cmd.type()),
                        "windowSeconds", b.windowSeconds(),
                        "actualOccurrences", b.occurrences()))));
        return block;
    }

    private List<Restriction> effectiveRestrictions(UUID examId) {
        List<Restriction> restrictions = restrictionRepository.findByExamId(examId);
        return (restrictions == null || restrictions.isEmpty()) ? Restriction.defaults() : restrictions;
    }

    private static int maxOccurrencesFor(List<Restriction> restrictions, RestrictionType type) {
        return restrictions.stream()
                .filter(restriction -> restriction.type() == type)
                .findFirst()
                .map(Restriction::maxOccurrences)
                .orElse(0);
    }

    /**
     * Traduce el tipo de restricción al evento de SafeGuard equivalente: WINDOW_BLUR
     * representa la pérdida de foco y NETWORK_DISCONNECT la desconexión.
     */
    private static SafeguardEventType toEventType(RestrictionType type) {
        return switch (type) {
            case TAB_SWITCH -> SafeguardEventType.TAB_SWITCH;
            case FOCUS_LOSS -> SafeguardEventType.WINDOW_BLUR;
            case COPY_PASTE -> SafeguardEventType.COPY_PASTE;
            case DISCONNECTION -> SafeguardEventType.NETWORK_DISCONNECT;
        };
    }
}
