package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenProvider tokenProvider;

    private RefreshAccessTokenUseCase useCase;

    private User user;
    private RefreshToken validToken;

    @BeforeEach
    void setUp() {
        useCase = new RefreshAccessTokenUseCase(refreshTokenRepository, userRepository, tokenProvider, 30L);

        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
        user = new User(UUID.randomUUID(), "Ana", "Ruiz", "ana@example.com",
                "hash", country, new ArrayList<>(), Role.ALUMNO);

        validToken = new RefreshToken(user.getId(), "token-viejo", OffsetDateTime.now().plusDays(5));
    }

    @Test
    void this_should_rotate_the_token_and_return_a_new_pair() {
        when(refreshTokenRepository.findByToken("token-viejo")).thenReturn(Optional.of(validToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token-nuevo");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthTokens result = useCase.execute("token-viejo");

        assertThat(result.accessToken()).isEqualTo("access-token-nuevo");
        assertThat(result.refreshToken()).isNotEqualTo("token-viejo"); 
        assertThat(validToken.isRevoked()).isTrue();
    }

    @Test
    void this_should_save_first_the_old_token_revocation_then_the_new_one() {
        when(refreshTokenRepository.findByToken("token-viejo")).thenReturn(Optional.of(validToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-nuevo");

        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(savedTokens.capture())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("token-viejo");

        var capturedValues = savedTokens.getAllValues();
        assertThat(capturedValues).hasSize(2);
        assertThat(capturedValues.get(0).getToken()).isEqualTo("token-viejo");
        assertThat(capturedValues.get(0).isRevoked()).isTrue();
        assertThat(capturedValues.get(1).getToken()).isNotEqualTo("token-viejo");
    }

    @Test
    void this_should_reject_a_token_that_does_not_exist_in_bd() {
        when(refreshTokenRepository.findByToken("token-fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("token-fantasma"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void this_should_reject_a_token_ya_revoked() {
        RefreshToken revokedToken = new RefreshToken(
                user.getId(), "token-revocado", OffsetDateTime.now().plusDays(5));
        revokedToken.revoke(); 

        when(refreshTokenRepository.findByToken("token-revocado")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> useCase.execute("token-revocado"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void this_should_reject_an_expired_token() {
        RefreshToken expiredToken = new RefreshToken(
                user.getId(), "token-expirado", OffsetDateTime.now().minusSeconds(1));

        when(refreshTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> useCase.execute("token-expirado"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}