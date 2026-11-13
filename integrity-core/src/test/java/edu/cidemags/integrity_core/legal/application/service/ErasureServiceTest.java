package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ErasureRequest;
import edu.cidemags.integrity_core.legal.domain.model.ErasureStatus;
import edu.cidemags.integrity_core.legal.domain.model.ErasureType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ErasureRequestRepository;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.StudentDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErasureServiceTest {

    @Mock
    private StudentDataRepository studentData;

    @Mock
    private ErasureRequestRepository requests;

    @InjectMocks
    private ErasureService service;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void hardEraseDeletesResponsesAndAlertsAndAnonymizesAudit() {
        when(requests.save(any(ErasureRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ErasureRequest request = service.eraseStudentData(studentId);

        verify(studentData).deleteResponsesByStudent(studentId);
        verify(studentData).deleteAlerts(studentId);
        verify(studentData).anonymizeAuditLogs(studentId);
        verify(studentData).softErase(studentId);
        assertThat(request.getErasureType()).isEqualTo(ErasureType.HARD);
        assertThat(request.getStatus()).isEqualTo(ErasureStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isNotNull();
    }

    @Test
    void softEraseOnlyMarksRows() {
        when(requests.save(any(ErasureRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ErasureRequest request = service.softEraseStudentData(studentId);

        verify(studentData).softErase(studentId);
        assertThat(request.getStatus()).isEqualTo(ErasureStatus.COMPLETED);
    }

    @Test
    void isErasedWhenNoDataRemains() {
        when(studentData.hasAnyData(studentId)).thenReturn(false);
        assertThat(service.isErased(studentId)).isTrue();
    }
}
