package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Phone;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.PhoneEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final CountryMapper countryMapper = new CountryMapper();
    private final PhoneMapper phoneMapper = new PhoneMapper();
    private final UserMapper mapper = new UserMapper(countryMapper, phoneMapper);

    @Test
    void toDomain_should_map_user_country_and_phones_together() {
        CountryEntity countryEntity = new CountryEntity(UUID.randomUUID(), "Mexico", "MEX");
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity(userId, countryEntity, "Ana", "Ruiz",
                "ana@example.com", "hashed-pw", Role.PROFESOR);

        PhoneEntity phoneEntity = new PhoneEntity(UUID.randomUUID(), userEntity, "555-1234", true);
        userEntity.setPhones(List.of(phoneEntity));

        User domain = mapper.toDomain(userEntity);

        assertThat(domain.getId()).isEqualTo(userId);
        assertThat(domain.getEmail()).isEqualTo("ana@example.com");
        assertThat(domain.getRole()).isEqualTo(Role.PROFESOR);
        assertThat(domain.getCountry().name()).isEqualTo("Mexico");
        assertThat(domain.getPhones()).hasSize(1);
        assertThat(domain.getPhones().get(0).getNumber()).isEqualTo("555-1234");
    }

    @Test
    void toDomain_should_map_a_user_with_no_phones_to_an_empty_list() {
        CountryEntity countryEntity = new CountryEntity(UUID.randomUUID(), "Mexico", "MEX");
        UserEntity userEntity = new UserEntity(UUID.randomUUID(), countryEntity, "Ana", "Ruiz",
                "ana@example.com", "hashed-pw", Role.ALUMNO);

        User domain = mapper.toDomain(userEntity);

        assertThat(domain.getPhones()).isEmpty();
    }

    @Test
    void toEntity_should_link_the_given_country_entity_and_convert_phones() {
        Country country = new Country(UUID.randomUUID(), "Colombia", "COL");
        List<Phone> phones = new ArrayList<>();
        phones.add(new Phone("1212121212", true));
        User domain = new User("Juan", "Perez", "juan.perez@example.com", "hash",
                country, phones, Role.ADMINISTRATIVO);

        CountryEntity countryEntity = new CountryEntity(country.id(), country.name(), country.isoCode());

        UserEntity entity = mapper.toEntity(domain, countryEntity);
        assertThat(entity.getCountry()).isSameAs(countryEntity);
        assertThat(entity.getEmail()).isEqualTo("juan.perez@example.com");
        assertThat(entity.getRole()).isEqualTo(Role.ADMINISTRATIVO);
        assertThat(entity.getPhones()).hasSize(1);
        assertThat(entity.getPhones().get(0).getPhoneNumber()).isEqualTo("1212121212");
    }

    @Test
    void toEntity_should_link_every_mapped_phone_back_to_the_same_user_entity() {
        
        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
        List<Phone> phones = new ArrayList<>();
        phones.add(new Phone("111", false));
        phones.add(new Phone("222", true));
        User domain = new User("Maria", "Fuentes", "maria.fuentes@example.com", "hash",
                country, phones, Role.ALUMNO);

        CountryEntity countryEntity = new CountryEntity(country.id(), country.name(), country.isoCode());

        UserEntity entity = mapper.toEntity(domain, countryEntity);

        assertThat(entity.getPhones()).hasSize(2);
        assertThat(entity.getPhones())
                .allSatisfy(phoneEntity -> assertThat(phoneEntity.getUser()).isSameAs(entity));
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null, null)).isNull();
    }
}