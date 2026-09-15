package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.CountryMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryRepositoryImplTest {

    @Mock
    private CountryJpaRepository jpaRepository;

    private final CountryMapper mapper = new CountryMapper();
    private CountryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CountryRepositoryImpl(jpaRepository, mapper);
    }

    @Test
    void findById_should_map_to_domain_when_present() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.of(new CountryEntity(id, "Mexico", "MEX")));

        Optional<Country> result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Mexico");
    }

    @Test
    void findById_should_return_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void findByIsoCode_should_map_to_domain_when_present() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findByIsoCode("COL")).thenReturn(Optional.of(new CountryEntity(id, "Colombia", "COL")));

        Optional<Country> result = repository.findByIsoCode("COL");

        assertThat(result).isPresent();
        assertThat(result.get().isoCode()).isEqualTo("COL");
    }

    @Test
    void findByIsoCode_should_return_empty_when_not_found() {
        when(jpaRepository.findByIsoCode("ZZZ")).thenReturn(Optional.empty());

        assertThat(repository.findByIsoCode("ZZZ")).isEmpty();
    }
}