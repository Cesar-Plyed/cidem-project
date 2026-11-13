package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.Dpia;
import edu.cidemags.integrity_core.legal.domain.model.DPIAStatus;
import edu.cidemags.integrity_core.legal.domain.model.RiskLevel;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DpiaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DPIAServiceTest {

    @Mock
    private DpiaRepository repository;

    @InjectMocks
    private DPIAService service;

    @Test
    void createDpiaStartsAsDraft() {
        when(repository.save(any(Dpia.class))).thenAnswer(inv -> inv.getArgument(0));

        Dpia dpia = service.createDPIA("Examen de Matematicas", "Descripcion", RiskLevel.MEDIUM);

        assertThat(dpia.getStatus()).isEqualTo(DPIAStatus.DRAFT);
        assertThat(dpia.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(dpia.getTitle()).contains("Matematicas");
    }

    @Test
    void reviewDpiaUpdatesStatusAndReviewedAt() {
        Dpia stored = new Dpia("Examen", "Descripcion", RiskLevel.HIGH, List.of("Cifrado"));
        when(repository.findById(any())).thenReturn(Optional.of(stored));
        when(repository.save(any(Dpia.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID dpoId = UUID.randomUUID();

        Dpia reviewed = service.reviewDPIA(UUID.randomUUID(), dpoId, DPIAStatus.APPROVED);

        assertThat(reviewed.getStatus()).isEqualTo(DPIAStatus.APPROVED);
        assertThat(reviewed.getReviewedBy()).isEqualTo(dpoId);
        assertThat(reviewed.getReviewedAt()).isNotNull();
    }

    @Test
    void pendingReviewReturnsUnderReviewDpias() {
        when(repository.findByStatusOrderByReviewedAtDesc(DPIAStatus.UNDER_REVIEW))
                .thenReturn(List.of(new Dpia("Examen", "x", RiskLevel.LOW, List.of())));

        assertThat(service.getPendingReviewView().dpias()).hasSize(1);
    }

    @Test
    void exportedPdfContainsDpiaSummary() {
        Dpia stored = new Dpia("Examen de Matematicas", "Tratamiento de datos", RiskLevel.MEDIUM,
                List.of("Cifrado en reposo"));
        when(repository.findById(any())).thenReturn(Optional.of(stored));

        byte[] pdf = service.exportPDF(UUID.randomUUID());
        String text = new String(pdf, StandardCharsets.ISO_8859_1);

        assertThat(text).startsWith("%PDF-1.4");
        assertThat(text).contains("Examen de Matematicas", "MEDIUM", "Cifrado en reposo", "EIPD");
    }
}
