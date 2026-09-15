package edu.cidemags.integrity_core.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void this_should_be_valid_if_not_is_revoked_and_has_not_expired() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "some-token-value", OffsetDateTime.now().plusDays(1));

        assertThat(token.isValid()).isTrue();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void this_should_be_invalid_if_expired(){
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "some-token-value", OffsetDateTime.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void this_should_be_invalid_after_revoking_it_even_though_it_has_not_expired(){
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "some-token-value", OffsetDateTime.now().plusDays(1));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

}
