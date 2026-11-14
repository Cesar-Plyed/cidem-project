package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.DataCapturePolicy;
import edu.cidemags.integrity_core.legal.domain.model.DataType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataCapturePolicyRepository extends JpaRepository<DataCapturePolicy, UUID> {

    List<DataCapturePolicy> findAllByOrderByDataTypeAsc();

    List<DataCapturePolicy> findByMandatoryTrueOrderByDataTypeAsc();

    Optional<DataCapturePolicy> findByDataType(DataType dataType);
}
