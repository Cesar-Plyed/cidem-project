package edu.cidemags.integrity_core.safeguard.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.shared.events.ExamCreatedEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardConfigurationRepository;

/**
 * Siembra la configuración por defecto de SafeGuard cuando se crea un examen
 * (flujo de activación automática). Sin configuración, el monitor del alumno
 * recibía 404 y se apagaba: SafeGuard nunca corría de forma real.
 * <p>
 * Los valores por defecto son la política "estricta": protección activa,
 * pantalla completa obligatoria y bloqueo de copiar/pegar. El profesor puede
 * ajustarlos después con PUT /api/safeguard/exams/{id}/config.
 */
@Component
public class ExamCreatedEventListener {

    private final SafeguardConfigurationRepository configurationRepository;

    public ExamCreatedEventListener(SafeguardConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    /** Listener transaccional: si la creación del examen se revierte, la configuración también. */
    @Transactional
    @EventListener
    public void onExamCreated(ExamCreatedEvent event) {
        if (event == null || event.examId() == null) {
            return;
        }
        if (configurationRepository.findByExamId(event.examId()).isPresent()) {
            return; // ya existe (p. ej. semilla manual o migración): no se pisa
        }
        SafeguardConfiguration config = new SafeguardConfiguration();
        config.setExamId(event.examId());
        config.setEnabled(true);
        config.setRequireFullscreen(true);
        config.setBlockCopyPaste(true);
        configurationRepository.saveAndFlush(config);
    }
}
