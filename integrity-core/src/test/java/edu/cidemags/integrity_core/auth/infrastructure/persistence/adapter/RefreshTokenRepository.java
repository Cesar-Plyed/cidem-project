package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.RefreshTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    @Mock
    private RefreshTokenJpaRepository jpaRepository;

    private final RefreshTokenMapper mapper = new RefreshTokenMapper();
    private RefreshTokenRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RefreshTokenRepositoryImpl(jpaRepository, mapper);
    }

    @Test
    void save_should_delegate_to_the_jpa_repository_and_return_the_mapped_domain_token() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = new RefreshToken(userId, "some-token", OffsetDateTime.now().plusDays(5));

        when(jpaRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = repository.save(token);

        assertThat(result.getToken()).isEqualTo("some-token");
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void findByToken_should_map_to_domain_when_present() {
        UUID userId = UUID.randomUUID();
        RefreshTokenEntity entity = new RefreshTokenEntity(
                UUID.randomUUID(), userId, "some-token", OffsetDateTime.now().plusDays(5), false);

        when(jpaRepository.findByToken("some-token")).thenReturn(Optional.of(entity));

        Optional<RefreshToken> result = repository.findByToken("some-token");

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByToken_should_return_empty_when_not_found() {
        when(jpaRepository.findByToken("no-existe")).thenReturn(Optional.empty());

        assertThat(repository.findByToken("no-existe")).isEmpty();
    }

    @Test
    void revokeAllByUserId_should_delegate_to_the_jpa_repository() {
        UUID userId = UUID.randomUUID();

        repository.revokeAllByUserId(userId);

        verify(jpaRepository, times(1)).revokeAllByUserId(userId);
    }
}