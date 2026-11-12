package edu.cidemags.integrity_core.sentinel.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuracion de Sentinel.
 *
 * <p>Las ventanas de escalamiento se declaran en {@code application.yml} bajo
 * {@code sentinel.escalation.*}. Los valores por defecto replican el spec 08
 * para que la aplicacion arranque aunque falte la seccion.</p>
 */
@ConfigurationProperties(prefix = "sentinel")
public record SentinelProperties(Escalation escalation) {

    public SentinelProperties {
        if (escalation == null) {
            escalation = new Escalation(null, null, null, null);
        }
    }

    public record Escalation(
        @DefaultValue("300s") Duration tabSwitchWindow,
        @DefaultValue("60s") Duration focusLossWindow,
        @DefaultValue("300s") Duration disconnectionWindow,
        @DefaultValue("120s") Duration copyPasteWindow
    ) {
        public Escalation {
            if (tabSwitchWindow == null) {
                tabSwitchWindow = Duration.ofMinutes(5);
            }
            if (focusLossWindow == null) {
                focusLossWindow = Duration.ofMinutes(1);
            }
            if (disconnectionWindow == null) {
                disconnectionWindow = Duration.ofMinutes(5);
            }
            if (copyPasteWindow == null) {
                copyPasteWindow = Duration.ofMinutes(2);
            }
        }
    }
}
