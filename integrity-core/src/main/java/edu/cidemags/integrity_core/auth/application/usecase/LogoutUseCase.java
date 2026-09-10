package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @Transactional
    public void execute(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);
        token.revoke();
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void executeForAllDevices(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}