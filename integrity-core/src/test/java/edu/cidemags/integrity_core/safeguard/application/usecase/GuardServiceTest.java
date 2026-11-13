package edu.cidemags.integrity_core.safeguard.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import edu.cidemags.integrity_core.safeguard.application.dto.GuardEventCommand;
import edu.cidemags.integrity_core.safeguard.domain.model.BlockEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionSeverity;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.port.RestrictionRepository;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardEventRepository;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos exigidos por la spec 20: con el umbral superado se publica exactamente un
 * {@link SafeguardBlockEvent}; con el umbral alcanzado (límite inclusivo) o con la
 * restricción deshabilitada, no se publica nada.
 */
@ExtendWith(MockitoExtension.class)
class GuardServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock private SafeguardService safeguardService;
    @Mock private SafeguardEventRepository eventRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private ApplicationEventPublisher publisher;

    private GuardService guardService;

    @BeforeEach
    void setUp() {
        guardService = new GuardService(safeguardService, eventRepository, restrictionRepository, publisher);
    }

    // ---------- helpers ----------

    private GuardEventCommand command(RestrictionType type) {
        return new GuardEventCommand(SESSION_ID, EXAM_ID, ATTEMPT_ID, STUDENT_ID,
                UUID.randomUUID(), type, Instant.now(), null);
    }

    private static Restriction restriction(RestrictionType type, boolean enabled, int maxOccurrences) {
        return Restriction.of(type, enabled, maxOccurrences, 300, RestrictionSeverity.LOW);
    }

    // ---------- tests ----------

    @Test
    void fourTabSwitches_overMaxThree_publishesExactlyOneBlockEvent() {
        when(restrictionRepository.findByExamId(EXAM_ID))
                .thenReturn(List.of(restriction(RestrictionType.TAB_SWITCH, true, 3)));
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH))
                .thenReturn(4L);

        Optional<BlockEvent> result = guardService.registerRestrictionEvent(command(RestrictionType.TAB_SWITCH));

        assertThat(result).isPresent();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SafeguardBlockEvent.class);
        SafeguardBlockEvent event = (SafeguardBlockEvent) captor.getValue();
        assertThat(event.source()).isEqualTo(SafeguardBlockEvent.SOURCE_SAFEGUARD);
        assertThat(event.examId()).isEqualTo(EXAM_ID);
        assertThat(event.attemptId()).isEqualTo(ATTEMPT_ID);
        assertThat(event.examSessionId()).isEqualTo(SESSION_ID);
        assertThat(event.studentId()).isEqualTo(STUDENT_ID);
        assertThat(event.payload())
                .containsEntry("type", "TAB_SWITCH")
                .containsEntry("maxOccurrences", 3)
                .containsEntry("windowSeconds", 300)
                .containsEntry("actualOccurrences", 4);
    }

    @Test
    void threeTabSwitches_withinMaxThree_allowedAndDoesNotPublish() {
        when(restrictionRepository.findByExamId(EXAM_ID))
                .thenReturn(List.of(restriction(RestrictionType.TAB_SWITCH, true, 3)));
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.TAB_SWITCH))
                .thenReturn(3L);

        Optional<BlockEvent> result = guardService.registerRestrictionEvent(command(RestrictionType.TAB_SWITCH));

        assertThat(result).isEmpty();
        verify(publisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void disabledFocusLoss_allowedAndDoesNotPublish() {
        when(restrictionRepository.findByExamId(EXAM_ID))
                .thenReturn(List.of(restriction(RestrictionType.FOCUS_LOSS, false, 1)));
        when(eventRepository.countBySessionIdAndEventType(SESSION_ID, SafeguardEventType.WINDOW_BLUR))
                .thenReturn(5L);

        Optional<BlockEvent> result = guardService.registerRestrictionEvent(command(RestrictionType.FOCUS_LOSS));

        assertThat(result).isEmpty();
        verify(publisher, never()).publishEvent(any(Object.class));
    }
}
