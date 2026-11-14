package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import edu.cidemags.integrity_core.legal.domain.entity.DataRetentionPolicy;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataRetentionPolicyRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    @Mock
    private DataRetentionPolicyRepository policyRepository;

    @Mock
    private StudentDataRepository studentData;

    private DataRetentionService service;

    @BeforeEach
    void setUp() {
        service = new DataRetentionService(policyRepository, studentData,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deleteExpiredDataDeletesResponsesBeforeCutoff() {
        Instant before = NOW.minusSeconds(3L * 365 * 24 * 3600);

        service.deleteExpiredData(before);

        verify(studentData).deleteResponsesBefore(before);
    }

    @Test
    void archiveDataMovesToArchivedResponses() {
        Instant before = NOW.minusSeconds(3L * 365 * 24 * 3600);

        service.archiveData(before);

        verify(studentData).archiveResponsesBefore(before);
    }

    @Test
    void scheduledCheckArchivesBeforeDeleting() {
        when(policyRepository.findFirstByActiveTrueOrderByRetentionStartDateDesc())
                .thenReturn(Optional.of(new DataRetentionPolicy(3, List.of("NAME", "EMAIL"))));

        service.scheduleRetentionCheck();

        verify(studentData).archiveResponsesBefore(any());
        verify(studentData).deleteResponsesBefore(any());
    }

    @Test
    void retentionInfoUsesActivePolicyAndNextMonthCutoff() {
        when(policyRepository.findFirstByActiveTrueOrderByRetentionStartDateDesc())
                .thenReturn(Optional.of(new DataRetentionPolicy(3, List.of("NAME", "EMAIL"))));

        var info = service.getRetentionInfo();

        assertThat(info.retentionYears()).isEqualTo(3);
        assertThat(info.dataTypes()).containsExactly("NAME", "EMAIL");
        assertThat(info.nextDeletionDate()).isEqualTo(Instant.parse("2026-12-01T02:00:00Z"));
    }

    @Test
    void retentionInfoFallsBackToDefaults() {
        when(policyRepository.findFirstByActiveTrueOrderByRetentionStartDateDesc())
                .thenReturn(Optional.empty());

        var info = service.getRetentionInfo();

        assertThat(info.retentionYears()).isEqualTo(3);
        assertThat(info.dataTypes()).isNotEmpty();
    }

    @Test
    void cutoffIsThreeYearsBack() {
        when(policyRepository.findFirstByActiveTrueOrderByRetentionStartDateDesc())
                .thenReturn(Optional.of(new DataRetentionPolicy(3, List.of("NAME"))));

        assertThat(service.cutoff()).isBefore(NOW.minusSeconds(3L * 365 * 24 * 3600 - 86400));
    }
}
