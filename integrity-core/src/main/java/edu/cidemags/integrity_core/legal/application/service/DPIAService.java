package edu.cidemags.integrity_core.legal.application.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.Dpia;
import edu.cidemags.integrity_core.legal.domain.model.DPIAStatus;
import edu.cidemags.integrity_core.legal.domain.model.RiskLevel;
import edu.cidemags.integrity_core.legal.infrastructure.pdf.SimplePdfWriter;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DpiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Evaluacion de impacto (EIPD/DPIA, Art. 35 GDPR, spec 50). */
@Service
public class DPIAService {

    private final DpiaRepository repository;

    public DPIAService(DpiaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Dpia createDPIA(String title, String description, RiskLevel riskLevel) {
        return createDPIA(title, description, riskLevel, List.of());
    }

    @Transactional
    public Dpia createDPIA(String title, String description, RiskLevel riskLevel,
                           List<String> mitigationMeasures) {
        return repository.save(new Dpia(title, description, riskLevel, mitigationMeasures));
    }

    @Transactional
    public Dpia reviewDPIA(UUID dpiId, UUID reviewedBy, DPIAStatus status) {
        Dpia dpia = repository.findById(dpiId)
                .orElseThrow(() -> new IllegalArgumentException("EIPD no encontrada: " + dpiId));
        dpia.review(reviewedBy, status);
        return repository.save(dpia);
    }

    @Transactional(readOnly = true)
    public List<Dpia> getPendingReview() {
        return repository.findByStatusOrderByReviewedAtDesc(DPIAStatus.UNDER_REVIEW);
    }

    @Transactional(readOnly = true)
    public LegalViews.DpiaList getPendingReviewView() {
        List<LegalViews.DpiaSummary> summaries = getPendingReview().stream()
                .map(d -> new LegalViews.DpiaSummary(d.getId(), d.getTitle(),
                        d.getRiskLevel().name(), d.getStatus().name()))
                .toList();
        return new LegalViews.DpiaList(summaries);
    }

    @Transactional(readOnly = true)
    public Dpia get(UUID dpiId) {
        return repository.findById(dpiId)
                .orElseThrow(() -> new IllegalArgumentException("EIPD no encontrada: " + dpiId));
    }

    @Transactional(readOnly = true)
    public byte[] exportPDF(UUID dpiId) {
        Dpia dpia = get(dpiId);
        List<String> lines = new ArrayList<>();
        lines.add("EIPD / DPIA - " + dpia.getTitle());
        lines.add("Estado: " + dpia.getStatus().name());
        lines.add("Riesgo: " + dpia.getRiskLevel().name());
        lines.add("");
        lines.add("Descripcion:");
        lines.addAll(wrap(dpia.getDescription()));
        lines.add("");
        lines.add("Medidas de mitigacion:");
        if (dpia.getMitigationMeasures().isEmpty()) {
            lines.add("- Sin medidas registradas");
        } else {
            dpia.getMitigationMeasures().forEach(measure -> lines.add("- " + measure));
        }
        lines.add("");
        lines.add("Revisado por: " + (dpia.getReviewedBy() == null ? "pendiente" : dpia.getReviewedBy()));
        lines.add("Revisado el: " + DateTimeFormatter.ISO_INSTANT.format(dpia.getReviewedAt()));
        return SimplePdfWriter.write(lines);
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("-");
            return lines;
        }
        for (String paragraph : text.split("\\R")) {
            lines.add(paragraph.length() > 90 ? paragraph.substring(0, 90) : paragraph);
        }
        return lines;
    }
}
