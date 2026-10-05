package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardEventEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SafeguardEventMapperTest {

    private final SafeguardEventMapper mapper = new SafeguardEventMapper();

    private static SafeguardEventEntity fullEntity() {
        SafeguardEventEntity entity = new SafeguardEventEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventId(UUID.randomUUID());
        entity.setEventType(SafeguardEventType.TAB_SWITCH);
        entity.setSeverity(2);
        entity.setRiskPoints(5);
        entity.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setMetadata("{\"tabCount\":3}");
        return entity;
    }

    @Test
    void toDomain_should_map_all_fields_and_bind_the_given_session() {
        SafeguardEventEntity entity = fullEntity();
        SafeguardSession session = new SafeguardSession();
        session.setId(UUID.randomUUID());

        SafeguardEvent domain = mapper.toDomain(entity, session);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getEventId()).isEqualTo(entity.getEventId());
        assertThat(domain.getEventType()).isEqualTo(SafeguardEventType.TAB_SWITCH);
        assertThat(domain.getSeverity()).isEqualTo(2);
        assertThat(domain.getRiskPoints()).isEqualTo(5);
        assertThat(domain.getOccurredAt()).isEqualTo(entity.getOccurredAt());
        assertThat(domain.getMetadata()).isEqualTo("{\"tabCount\":3}");
        assertThat(domain.getSession()).isSameAs(session);
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null, null)).isNull();
    }

    @Test
    void toEntity_should_map_business_fields_and_leave_id_to_jpa() {
        SafeguardEvent domain = new SafeguardEvent();
        domain.setEventId(UUID.randomUUID());
        domain.setEventType(SafeguardEventType.COPY_PASTE);
        domain.setSeverity(3);
        domain.setRiskPoints(10);

        SafeguardEventEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isNull(); // la genera JPA
        assertThat(entity.getEventId()).isEqualTo(domain.getEventId());
        assertThat(entity.getEventType()).isEqualTo(SafeguardEventType.COPY_PASTE);
        assertThat(entity.getRiskPoints()).isEqualTo(10);
        assertThat(entity.getSession()).isNull(); // la resuelve el adapter
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_should_write_business_fields_but_keep_id_session_and_createdAt() {
        SafeguardEventEntity managed = fullEntity();
        SafeguardSessionEntity originalSession = new SafeguardSessionEntity();
        originalSession.setId(UUID.randomUUID());
        managed.setSession(originalSession);
        UUID originalId = managed.getId();
        UUID originalEventId = managed.getEventId(); // inmutable tras persistir (updatable=false)

        SafeguardEvent domain = new SafeguardEvent();
        domain.setEventId(managed.getEventId());
        domain.setEventType(SafeguardEventType.MULTIPLE_FACES);
        domain.setSeverity(5);
        domain.setRiskPoints(35);
        domain.setOccurredAt(Instant.parse("2026-02-02T00:00:00Z"));

        mapper.updateEntity(domain, managed);

        assertThat(managed.getEventType()).isEqualTo(SafeguardEventType.MULTIPLE_FACES);
        assertThat(managed.getSeverity()).isEqualTo(5);
        assertThat(managed.getRiskPoints()).isEqualTo(35);

        assertThat(managed.getId()).isEqualTo(originalId);
        assertThat(managed.getEventId()).isEqualTo(originalEventId);
        assertThat(managed.getSession()).isSameAs(originalSession);
        assertThat(ReflectionTestUtils.getField(managed, "createdAt")).isNull(); // @PrePersist lo llena
    }
}
