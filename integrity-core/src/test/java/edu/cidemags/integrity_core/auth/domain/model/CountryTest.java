package edu.cidemags.integrity_core.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CountryTest {

    @Test
    void this_should_create_country_with_valid_data() {
        Country country = new Country(UUID.randomUUID(), "Mexico", "MEX");

        assertThat(country.name()).isEqualTo("Mexico");
        assertThat(country.isoCode()).isEqualTo("MEX");
    }

    @Test
    void this_should_reject_null_name() {
        assertThatThrownBy(() -> new Country(UUID.randomUUID(), null, "MEX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pais");
    }

    @Test
    void this_should_reject_blank_name() {
        assertThatThrownBy(() -> new Country(UUID.randomUUID(), "   ", "MEX"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void this_should_reject_iso_code_too_long() {
        assertThatThrownBy(() -> new Country(UUID.randomUUID(), "Mexico", "MEXX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iso");
    }

    @Test
    void this_should_accept_iso_code_of_exactly_three_characters() {
        Country country = new Country(UUID.randomUUID(), "Colombia", "COL");

        assertThat(country.isoCode()).isEqualTo("COL");
    }
}

