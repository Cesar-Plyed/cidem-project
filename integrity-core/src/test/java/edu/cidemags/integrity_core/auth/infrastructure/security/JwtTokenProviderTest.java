package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidAccessTokenException;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "a-super-secret-duper-test-key-for-test";

    private User buildUser() {
        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");
        return new User(UUID.randomUUID(), "Ana", "Ruiz", "ana@example.com",
                "hash", country, new ArrayList<>(), Role.PROFESOR);
    }

    @Test
    void this_should_generate_a_token_and_be_able_to_validate_it_afterwards() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 15);
        User user = buildUser();

        String token = provider.generateAccessToken(user);
        TokenProvider.AccessTokenClaims claims = provider.validateAccessToken(token);

        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.email()).isEqualTo(user.getEmail());
        assertThat(claims.role()).isEqualTo(Role.PROFESOR);
    }

    @Test
    void this_should_reject_a_token_with_invalid_text() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 15);

        assertThatThrownBy(() -> provider.validateAccessToken("esto-no-es-un-jwt-valido"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void this_should_reject_a_signed_token_with_another_key() {
        JwtTokenProvider providerOriginal = new JwtTokenProvider(TEST_SECRET, 15);
        JwtTokenProvider providerAtacante = new JwtTokenProvider(
                "otra-clave-completamente-distinta-32-caracteres", 15);

        String tokenFalsificado = providerAtacante.generateAccessToken(buildUser());

        assertThatThrownBy(() -> providerOriginal.validateAccessToken(tokenFalsificado))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void this_should_reject_an_expired_token() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, -1);

        String tokenExpirado = provider.generateAccessToken(buildUser());

        assertThatThrownBy(() -> provider.validateAccessToken(tokenExpirado))
                .isInstanceOf(InvalidAccessTokenException.class);
    }
}