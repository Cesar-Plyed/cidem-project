package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CountryMapperTest {

    private final CountryMapper mapper = new CountryMapper();

    @Test
    void toDomain_should_map_all_fields_correctly() {
        UUID id = UUID.randomUUID();
        CountryEntity entity = new CountryEntity(id, "Mexico", "MEX");

        Country domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("Mexico");
        assertThat(domain.isoCode()).isEqualTo("MEX");
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_should_map_all_fields_correctly() {
        UUID id = UUID.randomUUID();
        Country domain = new Country(id, "Colombia", "COL");

        CountryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Colombia");
        assertThat(entity.getIsoCode()).isEqualTo("COL");
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}