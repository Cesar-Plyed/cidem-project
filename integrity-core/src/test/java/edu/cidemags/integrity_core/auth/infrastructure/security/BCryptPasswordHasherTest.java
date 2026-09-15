package edu.cidemags.integrity_core.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import edu.cidemags.integrity_core.auth.domain.service.BCryptPasswordHasher;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

    @Test
    void hash_should_never_return_the_raw_password_as_is() {
        String hash = hasher.hash("my-secret-password");

        assertThat(hash).isNotEqualTo("my-secret-password").startsWith("$2");
    }

    @Test
    void hash_should_produce_a_different_value_each_time_even_for_the_same_password(){
        String hash1 = hasher.hash("same-password");
        String hash2 = hasher.hash("same-password");

        assertThat(hash1).isNotEqualTo(hash2);

        assertThat(hasher.matches("same-password", hash1)).isTrue();
        assertThat(hasher.matches("same-password", hash2)).isTrue();
    }

}
