package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.CountryMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.PhoneMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.UserMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.UserJpaRepository;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;
    @Mock
    private CountryJpaRepository countryJpaRepository;

    private final UserMapper userMapper = new UserMapper(new CountryMapper(), new PhoneMapper());

    private UserRepositoryImpl repositoryImpl;

    private final UUID countryId = UUID.randomUUID();
    private final Country country = new Country(countryId, "Mexico", "MEX");

    @BeforeEach
    void startUp() {
        repositoryImpl = new UserRepositoryImpl(userJpaRepository, countryJpaRepository, userMapper);
    }

    @Test
    void save_should_use_a_country_reference_instead_of_fetching_or_saving_the_country() {
        User user = new User("Pablo", "Peralta", "pablo.peralta@example.com", "hash", country, new ArrayList<>(),
                Role.ALUMNO);

        CountryEntity countryEntity = new CountryEntity(countryId, "Mexico", "MEX");

        when(countryJpaRepository.getReferenceById(countryId)).thenReturn(countryEntity);
        when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        repositoryImpl.save(user);

        verify(countryJpaRepository).getReferenceById(countryId);
        verify(countryJpaRepository, never()).findById(countryId);
        verify(countryJpaRepository, never()).save(any());
    }

    @Test
    void save_should_delegate_to_the_jpa_repository_and_return_the_mapped_domain_user() {
        User user = new User("Pablo", "Peralta", "pablo.peralta@example.com", "hash", country, new ArrayList<>(),
                Role.ALUMNO);

        when(countryJpaRepository.getReferenceById(countryId))
                .thenReturn(new CountryEntity(countryId, "Mexico", "MEX"));
        when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = repositoryImpl.save(user);

        assertThat(result.getEmail()).isEqualTo("pablo.peralta@example.com");
        verify(userJpaRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void findById_should_map_the_entity_to_domain_when_present() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity(userId, new CountryEntity(countryId, "Mexico", "MEX"), "Marco", "Polo",
                "marco.polo@example.com", "hash", Role.ALUMNO);

        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(entity));

        Optional<User> result = repositoryImpl.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("marco.polo@example.com");
    }

    @Test
    void findById_should_return_empty_when_not_found() {
        UUID userId = UUID.randomUUID();

        when(userJpaRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(userJpaRepository.findById(userId)).isEmpty();
    }

    @Test
    void findByEmail_should_delegate_to_the_jpa_repositroy() {
        when(userJpaRepository.findByEmail("marco.polo@example.com")).thenReturn(Optional.empty());
        repositoryImpl.findByEmail("marco.polo@example.com");

        verify(userJpaRepository).findByEmail("marco.polo@example.com");
    }

    @Test
    void existsByEmail_should_delegate_to_the_jpa_repository() {
        when(userJpaRepository.existsByEmail("marco.polo@example.com")).thenReturn(true);

        boolean result = repositoryImpl.existsByEmail("marco.polo@example.com");

        assertThat(result).isTrue();
        verify(userJpaRepository).existsByEmail("marco.polo@example.com");
    }
}
