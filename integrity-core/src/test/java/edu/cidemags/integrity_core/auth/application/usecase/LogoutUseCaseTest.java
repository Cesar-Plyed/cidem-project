package edu.cidemags.integrity_core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;

@ExtendWith (MockitoExtension.class)
class LogoutUseCaseTest {
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
 
    private LogoutUseCase useCase;
 
    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(refreshTokenRepository);
    }

    @Test
    void this_should_revoke_the_given_refresh_token(){
        UUID userId = UUID.randomUUID();
        RefreshToken token = new RefreshToken(userId, "user-referesh-token", OffsetDateTime.now().plusDays(5));

        when(refreshTokenRepository.findByToken("user-refresh-token")).thenReturn(Optional.of(token));

        ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);

        when(refreshTokenRepository.save(savedToken.capture())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("user-refresh-token");

        assertThat(savedToken.getValue().isRevoked()).isTrue();
    }

    @Test 
    void this_should_launch_an_exeption_if_the_token_not_exisit(){
        when(refreshTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("ghost-token")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void executeForAllDevices_should_delegate_direct_to_the_repository(){
        UUID userId = UUID.randomUUID();

        useCase.executeForAllDevices(userId);

        verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
    }
}
