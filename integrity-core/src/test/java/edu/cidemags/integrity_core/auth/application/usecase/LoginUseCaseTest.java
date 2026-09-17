package edu.cidemags.integrity_core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import edu.cidemags.integrity_core.auth.application.dto.LoginCommand;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidCredentialsException;
import edu.cidemags.integrity_core.auth.domain.model.AuthTokens;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.PasswordHasher;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;

@ExtendWith (MockitoExtension.class)
class LoginUseCaseTest {

    @Mock 
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private TokenProvider tokenProvider;

    private LoginUseCase useCase;

    private User existingUser;

    @BeforeEach
    void setUp(){
        useCase = new LoginUseCase(userRepository, refreshTokenRepository, passwordHasher, tokenProvider, 30L);

        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
        existingUser = new User(UUID.randomUUID(), "Ana", "Ruiz", "ana@example.com", "hashed-password-in-db", country, new ArrayList<>(), Role.ALUMNO);
    }

    @Test
    void this_should_return_tokens_when_credentials_are_correct() {
        LoginCommand command = new LoginCommand("ana@example.com", "password123");
 
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches("password123", "hashed-password-in-db")).thenReturn(true);
        when(tokenProvider.generateAccessToken(existingUser)).thenReturn("jwt-generado");
 
        AuthTokens tokens = useCase.execute(command);
 
        assertThat(tokens.accessToken()).isEqualTo("jwt-generado");
        assertThat(tokens.refreshToken()).isNotBlank();
 
        verify(refreshTokenRepository).save(any());
    }
 
    @Test
    void this_should_release_invalid_credentials_if_the_email_does_not_exist() {
        LoginCommand command = new LoginCommand("noexiste@example.com", "cualquier-cosa");
 
        when(userRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCredentialsException.class);
    }
 
    @Test
    void this_should_release_invalid_credentials_if_the_password_does_not_match() {
        LoginCommand command = new LoginCommand("ana@example.com", "password-incorrecto");
 
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches("password-incorrecto", "hashed-password-in-db")).thenReturn(false);
 
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCredentialsException.class);
    }
 
    @Test
    void this_both_failures_should_release_EXACTLY_the_same_exception_and_message() {
 
        when(userRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());
        String mensajeEmailInexistente = catchMessage(
                () -> useCase.execute(new LoginCommand("noexiste@example.com", "x")));
 
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches("mal", "hashed-password-in-db")).thenReturn(false);
        String mensajePasswordIncorrecto = catchMessage(
                () -> useCase.execute(new LoginCommand("ana@example.com", "mal")));
 
        assertThat(mensajeEmailInexistente).isEqualTo(mensajePasswordIncorrecto);
    }

    private String catchMessage(Runnable action) {
        try {
            action.run();
            throw new IllegalStateException("Se esperaba que lanzara una excepcion y no lo hizo");
        } catch (InvalidCredentialsException ex) {
            return ex.getMessage();
        }
    }
}
