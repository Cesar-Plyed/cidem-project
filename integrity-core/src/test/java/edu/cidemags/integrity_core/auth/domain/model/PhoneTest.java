package edu.cidemags.integrity_core.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneTest {

    @Test
    void this_should_create_phone_with_valid_number() {
        Phone phone = new Phone("4444444444", true);

        assertThat(phone.getNumber()).isEqualTo("4444444444");
        assertThat(phone.isPrimary()).isTrue();
        assertThat(phone.getId()).isNull();
    }

    @Test
    void this_should_reject_null_number() {
        assertThatThrownBy(() -> new Phone(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefono");
    }

    @Test
    void this_should_reject_number_on_blank() {
        assertThatThrownBy(() -> new Phone("   ", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void the_id_builder_should_also_validate_the_number() {
        assertThatThrownBy(() -> new Phone(UUID.randomUUID(), "", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markAsPrimary_y_unmarkPrimary_they_should_change_the_state() {
        Phone phone = new Phone("4444444444", false);
        assertThat(phone.isPrimary()).isFalse();

        phone.markAsPrimary();
        assertThat(phone.isPrimary()).isTrue();

        phone.unmarkPrimary();
        assertThat(phone.isPrimary()).isFalse();
    }

    @Test
    void two_phones_with_the_same_number_should_be_equal_regardless_of_id_or_primary_flag() {
        Phone phone1 = new Phone(UUID.randomUUID(), "1212121212", true);
        Phone phone2 = new Phone(UUID.randomUUID(), "1212121212", false);

        assertThat(phone1).isEqualTo(phone2);
        assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
    }

    @Test
    void two_phones_with_different_numbers_should_not_be_equal() {
        Phone phone1 = new Phone("1212121212", false);
        Phone phone2 = new Phone("1212121213", false);

        assertThat(phone1).isNotEqualTo(phone2);
    }
}