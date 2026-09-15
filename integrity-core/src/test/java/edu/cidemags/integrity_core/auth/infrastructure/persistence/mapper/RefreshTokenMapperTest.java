package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMapperTest {

    private final RefreshTokenMapper mapper = new RefreshTokenMapper();

    @Test
    void toDomain_should_map_all_fields_including_revoked_flag() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(5);
        RefreshTokenEntity entity = new RefreshTokenEntity(id, userId, "some-token", expiresAt, true);

        RefreshToken domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getToken()).isEqualTo("some-token");
        assertThat(domain.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(domain.isRevoked()).isTrue();
    }

    @Test
    void toEntity_should_map_all_fields_including_revoked_flag() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(30);
        RefreshToken domain = new RefreshToken(userId, "another-token", expiresAt);
        domain.revoke();

        RefreshTokenEntity entity = mapper.toEntity(domain);

        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getToken()).isEqualTo("another-token");
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.isRevoked()).isTrue();
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}