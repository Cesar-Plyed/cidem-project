package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.exception.UserNotFoundException;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshAccessTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final long refreshTokenExpirationDays;

    public RefreshAccessTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                                     UserRepository userRepository,
                                     TokenProvider tokenProvider,
                                     @Value("${security.jwt.refresh-token-expiration-days:30}") long refreshTokenExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthTokens execute(String refreshTokenValue) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!currentToken.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(currentToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(currentToken.getUserId()));

        currentToken.revoke();
        refreshTokenRepository.save(currentToken);

        String newAccessToken = tokenProvider.generateAccessToken(user);
        RefreshToken newRefreshToken = new RefreshToken(
                user.getId(),
                UUID.randomUUID().toString(),
                OffsetDateTime.now().plusDays(refreshTokenExpirationDays)
        );
        refreshTokenRepository.save(newRefreshToken);

        return new AuthTokens(newAccessToken, newRefreshToken.getToken());
    }
}