package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.DataCapturePolicy;
import edu.cidemags.integrity_core.legal.domain.model.DataType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataCapturePolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataMinimizationServiceTest {

    @Mock
    private DataCapturePolicyRepository repository;

    @Test
    void anonymizeDataIsSha256OfIdAndSalt() throws Exception {
        DataMinimizationService service = new DataMinimizationService(repository, "salt");
        UUID studentId = new UUID(0, 123);

        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((studentId + ":salt").getBytes(StandardCharsets.UTF_8)));

        assertThat(service.anonymizeData(studentId)).isEqualTo(expected);
        assertThat(service.anonymizeData(studentId)).hasSize(64);
    }

    @Test
    void maskIPHidesLastOctet() {
        DataMinimizationService service = new DataMinimizationService(repository, "salt");

        assertThat(service.maskIP("192.168.1.1")).isEqualTo("192.168.1.xxx");
        assertThat(service.maskIP("10.0.0.255")).isEqualTo("10.0.0.xxx");
        assertThat(service.maskIP(null)).isNull();
        assertThat(service.maskIP("")).isEmpty();
    }

    @Test
    void dataTypesSplitRequiredAndOptional() {
        when(repository.findAllByOrderByDataTypeAsc()).thenReturn(List.of(
                new DataCapturePolicy(DataType.NAME, 1095, true),
                new DataCapturePolicy(DataType.EMAIL, 1095, true),
                new DataCapturePolicy(DataType.IP_ADDRESS, 90, false)));

        var info = new DataMinimizationService(repository, "salt").getDataTypesInfo();

        assertThat(info.required()).containsExactly("NAME", "EMAIL");
        assertThat(info.optional()).containsExactly("IP_ADDRESS");
        assertThat(info.retentionDays()).containsEntry("IP_ADDRESS", 90);
    }

    @Test
    void requiredDataTypesComeFromMandatoryPolicies() {
        when(repository.findByMandatoryTrueOrderByDataTypeAsc())
                .thenReturn(List.of(new DataCapturePolicy(DataType.NAME, 1095, true)));

        assertThat(new DataMinimizationService(repository, "salt").getRequiredDataTypes())
                .containsExactly("NAME");
    }
}
