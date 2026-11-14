package edu.cidemags.integrity_core.safeguard.application.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cidemags.integrity_core.safeguard.domain.model.BlockEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionSeverity;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos exigidos por la spec 19: el umbral es inclusivo (maxOccurrences eventos
 * son aceptables, el siguiente bloquea) y las restricciones deshabilitadas se
 * ignoran. Se cubre además el tipo sin restricción configurada.
 */
class RestrictionValidatorTest {

    private final RestrictionValidator validator = new RestrictionValidator();

    private static Restriction tabSwitch(int maxOccurrences) {
        return Restriction.of(RestrictionType.TAB_SWITCH, true, maxOccurrences, 300, RestrictionSeverity.LOW);
    }

    @Test
    void threeTabSwitches_withinMaxThree_allowed() {
        assertThat(validator.validate(List.of(tabSwitch(3)), RestrictionType.TAB_SWITCH, 3))
                .isEmpty();
    }

    @Test
    void fourTabSwitches_overMaxThree_blockedWithReason() {
        var result = validator.validate(List.of(tabSwitch(3)), RestrictionType.TAB_SWITCH, 4);

        assertThat(result).isPresent();
        BlockEvent block = result.get();
        assertThat(block.type()).isEqualTo(RestrictionType.TAB_SWITCH);
        assertThat(block.occurrences()).isEqualTo(4);
        assertThat(block.windowSeconds()).isEqualTo(300);
        assertThat(block.reason()).contains("TAB_SWITCH", "3", "300");
    }

    @Test
    void disabledFocusLoss_isIgnoredAndNeverBlocks() {
        Restriction disabled = Restriction.of(RestrictionType.FOCUS_LOSS, false, 1, 60, RestrictionSeverity.MEDIUM);

        assertThat(validator.validate(List.of(disabled), RestrictionType.FOCUS_LOSS, 99))
                .isEmpty();
    }

    @Test
    void noRestrictionForType_allowed() {
        assertThat(validator.validate(List.of(tabSwitch(3)), RestrictionType.COPY_PASTE, 99))
                .isEmpty();
    }
}
