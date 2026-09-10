package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.application.dto.LoginCommand;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidCredentialsException;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.PasswordHasher;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

public class LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final long refreshTokenExpirationDays;

    public LoginUseCase(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordHasher passwordHasher,
                        TokenProvider tokenProvider,
                        @Value("${security.jwt.refresh-token-expiration-days:30}") long refreshTokenExpirationDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthTokens execute(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.rawPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = tokenProvider.generateAccessToken(user);

        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                UUID.randomUUID().toString(),
                OffsetDateTime.now().plusDays(refreshTokenExpirationDays)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthTokens(accessToken, refreshToken.getToken());
    }

}
