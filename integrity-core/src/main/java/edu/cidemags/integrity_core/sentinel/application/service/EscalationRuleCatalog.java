package edu.cidemags.integrity_core.sentinel.application.service;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.model.EscalationRule;
import edu.cidemags.integrity_core.sentinel.infrastructure.config.SentinelProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Reglas base de escalamiento definidas en el spec 08. Las ventanas vienen de
 * {@link SentinelProperties}; los umbrales y severidades son fijos.
 */
@Component
public class EscalationRuleCatalog {

    public static final String TAB_SWITCH_EVENT = "TAB_OR_APP_SWITCH";
    public static final String FOCUS_LOSS_EVENT = "WINDOW_FOCUS_LOST";
    public static final String DISCONNECTION_EVENT = "DISCONNECTION";
    public static final String COPY_PASTE_EVENT = "COPY_PASTE";

    private final SentinelProperties properties;

    public EscalationRuleCatalog(SentinelProperties properties) {
        this.properties = properties;
    }

    public List<EscalationRule> all() {
        SentinelProperties.Escalation escalation = properties.escalation();
        return List.of(
            new EscalationRule("TAB_SWITCH", TAB_SWITCH_EVENT, 3,
                escalation.tabSwitchWindow(), AlertSeverity.LOW),
            new EscalationRule("FOCUS_LOSS", FOCUS_LOSS_EVENT, 5,
                escalation.focusLossWindow(), AlertSeverity.MEDIUM),
            new EscalationRule("DISCONNECTION", DISCONNECTION_EVENT, 2,
                escalation.disconnectionWindow(), AlertSeverity.HIGH),
            new EscalationRule("COPY_PASTE", COPY_PASTE_EVENT, 4,
                escalation.copyPasteWindow(), AlertSeverity.MEDIUM)
        );
    }

    public Optional<EscalationRule> forEventType(String eventType) {
        if (eventType == null) {
            return Optional.empty();
        }
        return all().stream()
            .filter(rule -> rule.eventType().equalsIgnoreCase(eventType))
            .findFirst();
    }

    public Optional<EscalationRule> forRuleName(String ruleName) {
        if (ruleName == null) {
            return Optional.empty();
        }
        return all().stream()
            .filter(rule -> rule.ruleName().equalsIgnoreCase(ruleName))
            .findFirst();
    }
}
