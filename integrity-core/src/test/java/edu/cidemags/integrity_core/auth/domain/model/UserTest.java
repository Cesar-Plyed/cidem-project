package edu.cidemags.integrity_core.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private Country aCountry(){
        return new Country(UUID.randomUUID(), "Mexico", "MEX");
    }

    @Test
    void this_should_create_users_with_all_fields_valid(){
        Country country = aCountry();

        User user =  new User("Ana Garcia", "Ruiz Reyes", "ana@example.com", "Arroz123", country, new ArrayList<>(), Role.ALUMNO);

        assertThat(user.getFirstNames()).isEqualTo("Ana Garcia");
        assertThat(user.getRole()).isEqualTo(Role.ALUMNO);
        assertThat(user.hasRole(Role.ALUMNO)).isTrue();

    }

    @Test
    void this_should_execute_exception_if_the_role_is_null(){
        Country country = aCountry();

        assertThatThrownBy( () ->
            new User("Ana Garcia", "Ruiz Reyes", "ana@example.com", "Arroz123", country, new ArrayList<>(), null)
        ).isInstanceOf(IllegalArgumentException.class) 
                .hasMessageContaining("rol");
    }



    @Test
    void this_should_mark_only_one_phone_as_primary_to_the_time(){
        User user = new User("Ana Garcia", "Ruiz Reyes", "ana@example.com", "Arroz123", aCountry(), new ArrayList<>(), Role.ALUMNO);
        user.addPhone("4444444444", true);

        user.addPhone("55555555555", true);

        assertThat(user.getPhones()).hasSize(2);
        assertThat(user.getPhones().get(0).isPrimary()).isFalse();
        assertThat(user.getPhones().get(1).isPrimary()).isTrue();
    }

    @Test
    void getPhones_should_back_a_not_modified_list() {
        User user = new User("Ana Garcia", "Ruiz Reyes", "ana@example.com", "Arroz123", aCountry(), new ArrayList<>(), Role.ALUMNO);

        assertThatThrownBy( () ->
                user.getPhones().add(new Phone("6666666666", false))
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
