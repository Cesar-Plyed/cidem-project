package edu.cidemags.integrity_core.sentinel.application.service;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.model.EscalationRule;
import edu.cidemags.integrity_core.sentinel.infrastructure.config.SentinelProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EscalationRuleCatalogTest {

    private final SentinelProperties properties = new SentinelProperties(
        new SentinelProperties.Escalation(
            Duration.ofSeconds(300),
            Duration.ofSeconds(60),
            Duration.ofSeconds(300),
            Duration.ofSeconds(120)
        )
    );
    private final EscalationRuleCatalog catalog = new EscalationRuleCatalog(properties);

    @Test
    void shouldDefineTheFourBaseRules() {
        List<EscalationRule> rules = catalog.all();

        assertThat(rules).hasSize(4);
        assertThat(rules).extracting(EscalationRule::ruleName)
            .containsExactly("TAB_SWITCH", "FOCUS_LOSS", "DISCONNECTION", "COPY_PASTE");
        assertThat(rules).extracting(EscalationRule::severity)
            .containsExactly(AlertSeverity.LOW, AlertSeverity.MEDIUM, AlertSeverity.HIGH, AlertSeverity.MEDIUM);
        assertThat(rules).extracting(EscalationRule::threshold)
            .containsExactly(3, 5, 2, 4);
    }

    @Test
    void shouldUseConfiguredWindows() {
        assertThat(catalog.forRuleName("TAB_SWITCH").orElseThrow().window())
            .isEqualTo(Duration.ofSeconds(300));
        assertThat(catalog.forRuleName("FOCUS_LOSS").orElseThrow().window())
            .isEqualTo(Duration.ofSeconds(60));
        assertThat(catalog.forRuleName("DISCONNECTION").orElseThrow().window())
            .isEqualTo(Duration.ofSeconds(300));
        assertThat(catalog.forRuleName("COPY_PASTE").orElseThrow().window())
            .isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void shouldResolveRuleByEventTypeIgnoringCase() {
        assertThat(catalog.forEventType("tab_or_app_switch"))
            .map(EscalationRule::ruleName)
            .contains("TAB_SWITCH");
        assertThat(catalog.forEventType("UNKNOWN_EVENT")).isEmpty();
    }
}
