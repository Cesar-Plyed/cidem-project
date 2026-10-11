package edu.cidemags.integrity_core.safeguard.application.listener;

import edu.cidemags.integrity_core.shared.events.ExamCreatedEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamCreatedEventListenerTest {

    @Test
    void seedsStrictDefaultsWhenExamHasNoConfiguration() {
        SafeguardConfigurationRepository repository = mock(SafeguardConfigurationRepository.class);
        ExamCreatedEventListener listener = new ExamCreatedEventListener(repository);
        UUID examId = UUID.randomUUID();
        when(repository.findByExamId(examId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(SafeguardConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        listener.onExamCreated(new ExamCreatedEvent(examId, "Examen", UUID.randomUUID(), OffsetDateTime.now()));

        org.mockito.ArgumentCaptor<SafeguardConfiguration> captor =
                org.mockito.ArgumentCaptor.forClass(SafeguardConfiguration.class);
        verify(repository).saveAndFlush(captor.capture());
        SafeguardConfiguration config = captor.getValue();
        assertEquals(examId, config.getExamId());
        assertTrue(config.isEnabled());
        // Sin estos dos flags el monitor del alumno no exigía pantalla completa ni bloqueaba copiar/pegar.
        assertTrue(config.isRequireFullscreen());
        assertTrue(config.isBlockCopyPaste());
        assertFalse(config.isRequireCamera());
        assertFalse(config.isAutoTerminateOnThreshold());
    }

    @Test
    void keepsAnExistingConfigurationUntouched() {
        SafeguardConfigurationRepository repository = mock(SafeguardConfigurationRepository.class);
        ExamCreatedEventListener listener = new ExamCreatedEventListener(repository);
        UUID examId = UUID.randomUUID();
        SafeguardConfiguration existing = new SafeguardConfiguration();
        existing.setExamId(examId);
        existing.setRequireFullscreen(false);
        when(repository.findByExamId(examId)).thenReturn(Optional.of(existing));

        listener.onExamCreated(new ExamCreatedEvent(examId, "Examen", UUID.randomUUID(), OffsetDateTime.now()));

        verify(repository, never()).saveAndFlush(any(SafeguardConfiguration.class));
    }
}
