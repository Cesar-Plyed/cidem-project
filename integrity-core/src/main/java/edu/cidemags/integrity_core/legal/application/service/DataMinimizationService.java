package edu.cidemags.integrity_core.legal.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.DataCapturePolicy;
import edu.cidemags.integrity_core.legal.domain.model.DataType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.DataCapturePolicyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Principio de minimizacion (spec 41). */
@Service
public class DataMinimizationService {

    private final DataCapturePolicyRepository repository;
    private final String salt;

    public DataMinimizationService(DataCapturePolicyRepository repository,
                                   @Value("${legal.anonymization.salt:cidem-integrity-default-salt}") String salt) {
        this.repository = repository;
        this.salt = salt;
    }

    @Transactional(readOnly = true)
    public List<String> getRequiredDataTypes() {
        return repository.findByMandatoryTrueOrderByDataTypeAsc().stream()
                .map(policy -> policy.getDataType().name())
                .toList();
    }

    @Transactional(readOnly = true)
    public LegalViews.DataTypesInfo getDataTypesInfo() {
        List<DataCapturePolicy> policies = repository.findAllByOrderByDataTypeAsc();
        List<String> required = policies.stream()
                .filter(DataCapturePolicy::isMandatory)
                .map(p -> p.getDataType().name())
                .toList();
        List<String> optional = policies.stream()
                .filter(p -> !p.isMandatory())
                .map(p -> p.getDataType().name())
                .toList();
        Map<String, Integer> retentionDays = new LinkedHashMap<>();
        policies.forEach(p -> retentionDays.put(p.getDataType().name(), p.getRetentionDays()));
        if (policies.isEmpty()) {
            for (DataType type : DataType.values()) {
                retentionDays.put(type.name(), 90);
                if (type == DataType.NAME || type == DataType.EMAIL) {
                    required.add(type.name());
                } else {
                    optional.add(type.name());
                }
            }
        }
        return new LegalViews.DataTypesInfo(required, optional, retentionDays);
    }

    /** SHA-256(studentId:salt) en hexadecimal. */
    public String anonymizeData(UUID studentId) {
        String input = studentId + ":" + salt;
        return sha256(input);
    }

    /** "192.168.1.1" -> "192.168.1.xxx" (se oculta el ultimo octeto). */
    public String maskIP(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return ipAddress;
        }
        String[] parts = ipAddress.trim().split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
        }
        // IPv6 o formato desconocido: se conserva el prefijo y se oculta el resto.
        int lastColon = ipAddress.lastIndexOf(':');
        return lastColon > 0 ? ipAddress.substring(0, lastColon + 1) + "xxxx" : "xxxx";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
