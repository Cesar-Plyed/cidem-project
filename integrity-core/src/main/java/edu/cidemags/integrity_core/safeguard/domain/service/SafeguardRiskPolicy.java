package edu.cidemags.integrity_core.safeguard.domain.service;

import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

@Component
public class SafeguardRiskPolicy {

    public record Assessment(int severity, int riskPoints) {}

    public Assessment assess(SafeguardEventType type, long priorSameTypeCount, SafeguardConfiguration config) {
        int severity = switch (type) {
            case WINDOW_BLUR, RIGHT_CLICK -> 1;
            case TAB_SWITCH, NETWORK_DISCONNECT -> 2;
            case FULLSCREEN_EXIT, COPY_PASTE, NO_FACE_DETECTED, MICROPHONE_DISABLED -> 3;
            case CAMERA_DISABLED, DEVICE_CHANGE -> 4;
            case MULTIPLE_FACES, SUSPICIOUS_ACTIVITY -> 5;
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        };

        if (type == SafeguardEventType.TAB_SWITCH && priorSameTypeCount + 1 > config.getMaxTabSwitches()) {
            severity = 4;
        }
        return new Assessment(severity, pointsFor(severity));
    }

    private static int pointsFor(int severity) {
        return switch (severity) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 35;
        };
    }
}