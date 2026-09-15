package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Phone;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.PhoneEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneMapperTest {

    private final PhoneMapper mapper = new PhoneMapper();

    private UserEntity anOwner() {
        CountryEntity country = new CountryEntity(UUID.randomUUID(), "Mexico", "MEX");
        return new UserEntity(UUID.randomUUID(), country, "Rodolfo", "Dias",
                "rodolfo.dias@example.com", "hashed-password", Role.ALUMNO);
    }

    @Test
    void toDomain_should_map_number_and_primary_flag() {
        UUID id = UUID.randomUUID();
        PhoneEntity entity = new PhoneEntity(id, anOwner(), "4444444444", true);

        Phone domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getNumber()).isEqualTo("4444444444");
        assertThat(domain.isPrimary()).isTrue();
    }

    @Test
    void toEntity_should_link_the_given_owner_user() {
        Phone domain = new Phone(UUID.randomUUID(), "2222222222", false);
        UserEntity owner = anOwner();

        PhoneEntity entity = mapper.toEntity(domain, owner);

        assertThat(entity.getUser()).isSameAs(owner);
        assertThat(entity.getPhoneNumber()).isEqualTo("2222222222");
        assertThat(entity.isPrimary()).isFalse();
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null, anOwner())).isNull();
    }
}