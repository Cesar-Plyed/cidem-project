package edu.cidemags.integrity_core.legal.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.DataRetentionPolicy;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataRetentionPolicyRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Politica de retencion y borrado programado (spec 38).
 *
 * <p>Primero archiva en {@code archived_responses} y despues elimina de
 * {@code exam_answers}: el borrado de la tabla operativa es irreversible.</p>
 */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);
    private static final int DEFAULT_RETENTION_YEARS = 3;
    private static final List<String> DEFAULT_DATA_TYPES = List.of(
            "NAME", "EMAIL", "IP_ADDRESS", "USER_AGENT", "CAMERA_VIDEO",
            "MICROPHONE_AUDIO", "SCREENSHOT", "TAB_SWITCH", "FOCUS_LOSS", "COPY_PASTE");

    private final DataRetentionPolicyRepository policyRepository;
    private final StudentDataRepository studentData;
    private final Clock clock;

    public DataRetentionService(DataRetentionPolicyRepository policyRepository,
                                StudentDataRepository studentData,
                                Clock clock) {
        this.policyRepository = policyRepository;
        this.studentData = studentData;
        this.clock = clock;
    }

    /** Ejecucion mensual: dia 1 a las 02:00 (spec 38). */
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void scheduleRetentionCheck() {
        Instant beforeDate = cutoff();
        int archived = archiveData(beforeDate);
        int deleted = deleteExpiredData(beforeDate);
        log.info("Retencion legal: {} respuestas archivadas y {} eliminadas (corte {})",
                archived, deleted, beforeDate);
    }

    @Transactional
    public int deleteExpiredData(Instant beforeDate) {
        return studentData.deleteResponsesBefore(beforeDate);
    }

    @Transactional
    public int archiveData(Instant beforeDate) {
        return studentData.archiveResponsesBefore(beforeDate);
    }

    @Transactional(readOnly = true)
    public LegalViews.RetentionInfo getRetentionInfo() {
        DataRetentionPolicy policy = activePolicy();
        int years = policy == null ? DEFAULT_RETENTION_YEARS : policy.getRetentionYears();
        List<String> types = policy == null || policy.getDataTypes().isEmpty()
                ? DEFAULT_DATA_TYPES : policy.getDataTypes();
        return new LegalViews.RetentionInfo(years, types, nextDeletionDate());
    }

    public Instant cutoff() {
        return ZonedDateTime.now(clock).minusYears(retentionYears()).toInstant();
    }

    private int retentionYears() {
        DataRetentionPolicy policy = activePolicy();
        return policy == null ? DEFAULT_RETENTION_YEARS : policy.getRetentionYears();
    }

    private DataRetentionPolicy activePolicy() {
        return policyRepository.findFirstByActiveTrueOrderByRetentionStartDateDesc().orElse(null);
    }

    /** Primer dia del mes siguiente a las 02:00 UTC. */
    public Instant nextDeletionDate() {
        LocalDate firstOfNextMonth = LocalDate.now(clock).withDayOfMonth(1).plusMonths(1);
        return firstOfNextMonth.atStartOfDay().plusHours(2).toInstant(ZoneOffset.UTC);
    }
}
