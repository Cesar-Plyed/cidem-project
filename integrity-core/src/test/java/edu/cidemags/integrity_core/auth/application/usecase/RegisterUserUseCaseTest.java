package edu.cidemags.integrity_core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.cidemags.integrity_core.auth.application.dto.RegisterUserCommand;
import edu.cidemags.integrity_core.auth.domain.exception.CountryNotFoundException;
import edu.cidemags.integrity_core.auth.domain.exception.EmailAlreadyRegisteredException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRoleAssignmentException;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.CountryRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.PasswordHasher;
import edu.cidemags.integrity_core.auth.domain.service.RegistrationCodeValidator;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {
    
    @Mock 
    private UserRepository userRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock 
    private PasswordHasher passwordHasher;
    @Mock 
    private RegistrationCodeValidator registrationCodeValidator;


    private RegisterUserUseCase useCase;

    private final UUID countryId = UUID.randomUUID();
    private final Country country = new Country(countryId, "Mexico", "MEX");

    @BeforeEach
    void setUp(){
        useCase = new RegisterUserUseCase(userRepository, countryRepository, passwordHasher, registrationCodeValidator);
    }

    @Test
    void this_should_register_student_correctly(){
        RegisterUserCommand command = new RegisterUserCommand("Ana", "Ruiz", "ana@example.com", "Arroz123", countryId, "4444444444", Role.ALUMNO, null);

        when(registrationCodeValidator.isValid(Role.ALUMNO, null)).thenReturn(true);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
        when(passwordHasher.hash("Arroz123")).thenReturn("hashed-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = useCase.execute(command);

        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getRole()).isEqualTo(Role.ALUMNO);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void should_consume_the_registration_code_only_after_a_successful_registration() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Juan", "Perez", "juan.perez@example.com", "password123",
                countryId, null, Role.PROFESOR, "valid-code");

        when(registrationCodeValidator.isValid(Role.PROFESOR, "valid-code")).thenReturn(true);
        when(userRepository.existsByEmail("juan.perez@example.com")).thenReturn(false);
        when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
        when(passwordHasher.hash("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        verify(registrationCodeValidator, times(1)).consume("valid-code");
    }

    @Test
    void should_never_consume_the_registration_code_when_registration_fails_afterwards() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Juan", "Perez", "juan.perez@example.com", "password123",
                countryId, null, Role.PROFESOR, "valid-code");

        when(registrationCodeValidator.isValid(Role.PROFESOR, "valid-code")).thenReturn(true);
        when(userRepository.existsByEmail("juan.perez@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(registrationCodeValidator, never()).consume(any());
    }

    @Test
    void this_should_reject_registration_if_the_email_already_exists(){
        RegisterUserCommand command = new RegisterUserCommand("Ana", "Ruiz", "ana.ruiz@example.com", "Arroz123", countryId, null, Role.ALUMNO, null);

        when(registrationCodeValidator.isValid(Role.ALUMNO, null)).thenReturn(true);
        when(userRepository.existsByEmail("ana.ruiz@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test 
    void this_should_reject_registration_if_the_country_does_not_exist(){
        RegisterUserCommand command = new RegisterUserCommand("Ana", "Ruiz", "ana.ruiz@example.com", "Arroz123", countryId, null, Role.ALUMNO, null);

        when(registrationCodeValidator.isValid(Role.ALUMNO, null)).thenReturn(true);
        when(userRepository.existsByEmail("ana.ruiz@example.com")).thenReturn(false);
        when(countryRepository.findById(countryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CountryNotFoundException.class);
    }

    @Test 
    void this_should_reject_teacher_registration_with_invalid_code(){
        RegisterUserCommand command = new RegisterUserCommand(  "Juan", "Perez", "juan@example.com", "password123", countryId, null, Role.PROFESOR, "wrong-code");

        when(registrationCodeValidator.isValid(Role.PROFESOR, "wrong-code")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(InvalidRoleAssignmentException.class);

        verify(userRepository, never()).existsByEmail(any());
        verify(countryRepository, never()).findById(any());
        verify(registrationCodeValidator, never()).consume(any());
    }

    @Test
    void executeAsAdmin_not_should_require_registration_code(){
        RegisterUserCommand command = new RegisterUserCommand("Admin", "Uno", "admin@example.com", "password123", countryId, null, Role.ADMINISTRATIVO, null);

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
        when(passwordHasher.hash("password123")).thenReturn("hased");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = useCase.executeAsAdmin(command);

        assertThat(result.getRole()).isEqualTo(Role.ADMINISTRATIVO);

        verifyNoInteractions(registrationCodeValidator);
    }

    @Test
    void  this_should_add_phone_as_primary_if_provided(){
        RegisterUserCommand command = new RegisterUserCommand("Ana", "Ruiz", "ana@example.com", "Arroz123", countryId, "4444444444", Role.ALUMNO, null);

        when(registrationCodeValidator.isValid(Role.ALUMNO, null)).thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(countryRepository.findById(countryId)).thenReturn(Optional.of(country));
        when(passwordHasher.hash(any())).thenReturn("hashed");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPhones()).hasSize(1);
        assertThat(capturedUser.getPhones().get(0).getNumber()).isEqualTo("4444444444");
        assertThat(capturedUser.getPhones().get(0).isPrimary()).isTrue();

    }
}
