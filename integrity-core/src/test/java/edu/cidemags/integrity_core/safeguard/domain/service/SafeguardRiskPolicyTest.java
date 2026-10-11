package edu.cidemags.integrity_core.safeguard.domain.service;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tabla de severidad/puntos de la política de riesgo: es el núcleo del cómputo de
 * riesgo y hasta ahora solo se afirmaba indirectamente desde SafeguardServiceTest.
 * Los valores esperados están escritos a mano (duplican la tabla a propósito) para
 * que un cambio en la política falle el test en lugar de seguirlo silenciosamente.
 */
class SafeguardRiskPolicyTest {

    private final SafeguardRiskPolicy policy = new SafeguardRiskPolicy();

    private static final Map<Integer, Integer> POINTS_BY_SEVERITY =
            Map.of(1, 2, 2, 5, 3, 10, 4, 20, 5, 35);

    private static SafeguardConfiguration config(int maxTabSwitches) {
        SafeguardConfiguration c = new SafeguardConfiguration();
        c.setMaxTabSwitches(maxTabSwitches);
        return c;
    }

    @ParameterizedTest(name = "{0} -> severidad {1}")
    @CsvSource({
            "WINDOW_BLUR,1",        "RIGHT_CLICK,1",
            "TAB_SWITCH,2",         "NETWORK_DISCONNECT,2",
            "FULLSCREEN_EXIT,3",    "COPY_PASTE,3",
            "NO_FACE_DETECTED,3",   "MICROPHONE_DISABLED,3",
            "CAMERA_DISABLED,4",    "DEVICE_CHANGE,4",
            "SCREEN_SHARE_STOPPED,4",
            "CAMERA_BLOCKED,4",     "MICROPHONE_ANOMALY,4",
            "MULTIPLE_FACES,5",     "SUSPICIOUS_ACTIVITY,5",
            "DEVTOOLS_OPENED,5"
    })
    void assess_assignsExpectedSeverityAndPoints(SafeguardEventType type, int expectedSeverity) {
        SafeguardRiskPolicy.Assessment a = policy.assess(type, 0, config(3));

        assertEquals(expectedSeverity, a.severity());
        assertEquals(POINTS_BY_SEVERITY.get(expectedSeverity), a.riskPoints());
    }

    @Test
    void tabSwitch_withinLimit_keepsSeverityTwo() {
        // maxTabSwitches = 2: el segundo intento (prior=1) aún cabe en el límite
        SafeguardRiskPolicy.Assessment a = policy.assess(SafeguardEventType.TAB_SWITCH, 1, config(2));

        assertEquals(2, a.severity());
        assertEquals(5, a.riskPoints());
    }

    @Test
    void tabSwitch_overLimit_escalatesToSeverityFour() {
        // prior=2 + 1 = 3 > 2 → escala a severidad 4 (20 puntos)
        SafeguardRiskPolicy.Assessment a = policy.assess(SafeguardEventType.TAB_SWITCH, 2, config(2));

        assertEquals(4, a.severity());
        assertEquals(20, a.riskPoints());
    }

    @Test
    void tabSwitch_escalationDoesNotApplyToOtherTypes() {
        // COPY_PASTE no tiene límite de repetición: prior alto no cambia nada
        SafeguardRiskPolicy.Assessment a = policy.assess(SafeguardEventType.COPY_PASTE, 99, config(0));

        assertEquals(3, a.severity());
        assertEquals(10, a.riskPoints());
    }

    @Test
    void devtoolsOpened_closedOrOpen_logsAndPenalizesOncePerAttempt() {
        // DEVTOOLS_OPENED no escala por repetición: prior alto no cambia la severidad
        SafeguardRiskPolicy.Assessment a = policy.assess(SafeguardEventType.DEVTOOLS_OPENED, 99, config(3));

        assertEquals(5, a.severity());
        assertEquals(35, a.riskPoints());
    }
}
